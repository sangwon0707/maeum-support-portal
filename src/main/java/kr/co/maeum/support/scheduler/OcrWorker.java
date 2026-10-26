package kr.co.maeum.support.scheduler;

import kr.co.maeum.support.common.JsonCodec;
import kr.co.maeum.support.integration.ocr.OcrProcessingException;
import kr.co.maeum.support.integration.ocr.OcrProvider;
import kr.co.maeum.support.integration.ocr.PaddleOcrProvider;
import kr.co.maeum.support.integration.ocr.SampleOcrProvider;
import kr.co.maeum.support.mapper.DocumentMapper;
import kr.co.maeum.support.mapper.OcrMapper;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.UUID;

@Component
public class OcrWorker {
    private static final Logger log = LoggerFactory.getLogger(OcrWorker.class);
    private final kr.co.maeum.support.storage.StorageGuard guard;
    private final OcrMapper jobs;
    private final DocumentMapper documents;
    private final OcrProvider provider;
    private final PaddleOcrProvider paddle;
    private final JsonCodec json;
    private final TransactionTemplate transaction;

    public OcrWorker(
            OcrMapper jobs,
            DocumentMapper documents,
            OcrProvider provider,
            PaddleOcrProvider paddle,
            JsonCodec json,
            PlatformTransactionManager manager,
            kr.co.maeum.support.storage.StorageGuard guard) {
        this.guard = guard;
        this.jobs = jobs;
        this.documents = documents;
        this.provider = provider;
        this.paddle = paddle;
        this.json = json;
        this.transaction = new TransactionTemplate(manager);
    }

    @Scheduled(fixedDelay = 1000)
    public void runPending() {
        try (var ignored = guard.use()) {
            processPending();
        } catch (Exception exception) {
            log.warn("OCR 실행 실패 type={}", exception.getClass().getSimpleName());
        }
    }

    private void processPending() {
        jobs.expireExhausted();
        for (var job : jobs.pending()) {
            String token = UUID.randomUUID().toString();
            if (!Boolean.TRUE.equals(
                    transaction.execute(status -> jobs.claim(job.getId(), token) == 1))) {
                continue;
            }
            try {
                // 외부 공급자 호출이 긴 DB 잠금을 잡지 않도록 claim과 완료 저장을 분리한다.
                var document = documents.find(job.getDocumentId());
                var extracted =
                        "SAMPLE".equals(job.getProviderMode())
                                ? provider.extract(document)
                                : paddle.extract(document, "LIVE_AI".equals(job.getProviderMode()));
                transaction.executeWithoutResult(
                        status -> {
                            if (jobs.finish(job.getId(), token) == 1) {
                                jobs.result(
                                        job.getId(),
                                        json.write(extracted.fields()),
                                        json.write(extracted.evidence()),
                                        json.write(extracted.raw()));
                            }
                        });
            } catch (OcrProcessingException exception) {
                jobs.fail(
                        job.getId(),
                        token,
                        exception.retryable()
                                && !"LIVE_AI".equals(job.getProviderMode())
                                && job.getAttempts() + 1 < 3,
                        exception.code());
            } catch (SampleOcrProvider.SampleRecognitionException exception) {
                jobs.fail(job.getId(), token, false, "SAMPLE_UNREADABLE");
            } catch (RuntimeException exception) {
                jobs.fail(
                        job.getId(),
                        token,
                        !"LIVE_AI".equals(job.getProviderMode()) && job.getAttempts() + 1 < 3,
                        "PROCESSING_ERROR");
                log.warn(
                        "OCR 작업 실패 jobId={} type={}",
                        job.getId(),
                        exception.getClass().getSimpleName());
            }
        }
    }
}
