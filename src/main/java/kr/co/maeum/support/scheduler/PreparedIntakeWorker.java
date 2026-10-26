package kr.co.maeum.support.scheduler;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import kr.co.maeum.support.mapper.IntakeMapper;
import kr.co.maeum.support.service.IntakeResultCache;
import kr.co.maeum.support.service.PreparedIntakeCatalog;
import kr.co.maeum.support.storage.StorageGuard;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** 타이머는 사전 결과를 단계별로 보여주는 표시이며 실제 OCR 진행률과 섞지 않는다. */
@Component
public class PreparedIntakeWorker {
    private static final Logger log = LoggerFactory.getLogger(PreparedIntakeWorker.class);
    private final IntakeMapper jobs;
    private final PreparedIntakeCatalog catalog;
    private final IntakeResultCache cache;
    private final StorageGuard guard;
    private final TransactionTemplate transaction;
    private final ObjectMapper json = new ObjectMapper();

    public PreparedIntakeWorker(
            IntakeMapper jobs,
            PreparedIntakeCatalog catalog,
            IntakeResultCache cache,
            StorageGuard guard,
            PlatformTransactionManager manager) {
        this.jobs = jobs;
        this.catalog = catalog;
        this.cache = cache;
        this.guard = guard;
        transaction = new TransactionTemplate(manager);
    }

    @Scheduled(fixedDelay = 500)
    public void runPending() {
        for (var job : jobs.replaying()) {
            long id = ((Number) job.get("id")).longValue();
            double elapsed = ((Number) job.get("elapsed_seconds")).doubleValue();
            if (elapsed < 12) {
                String stage =
                        elapsed < 3
                                ? "LOADING"
                                : elapsed < 6 ? "READING" : elapsed < 9 ? "IMAGES" : "ASSEMBLING";
                int progress = elapsed < 3 ? 10 : elapsed < 6 ? 35 : elapsed < 9 ? 65 : 85;
                String detail =
                        elapsed < 3
                                ? "저장된 문서 구성 불러오는 중"
                                : elapsed < 6
                                        ? "페이지별 인식 결과 불러오는 중"
                                        : elapsed < 9 ? "분할 문서·사진 자료 불러오는 중" : "검토 화면 구성 중";
                jobs.replayProgress(id, stage, progress, detail);
                continue;
            }
            try (var ignored = guard.use()) {
                transaction.executeWithoutResult(
                        status -> {
                            var locked = jobs.lock(id);
                            if (locked == null || !"PROCESSING".equals(locked.get("status")))
                                return;
                            var sources = jobs.sources(id);
                            String code = catalog.match(locked.get("layout").toString(), sources);
                            if (code == null) throw new IllegalStateException("준비된 문서가 아닙니다.");
                            try {
                                String previous = cache.reuse(id, sources);
                                var result =
                                        previous == null
                                                ? catalog.materialize(code, sources.getFirst())
                                                : json.readTree(previous);
                                ((ObjectNode) result).put("executionMode", "PREPARED");
                                jobs.finish(id, "prepared", json.writeValueAsString(result));
                            } catch (java.io.IOException exception) {
                                throw new IllegalStateException(exception);
                            }
                        });
            } catch (Exception exception) {
                jobs.fail(id, "prepared", "PREPARED_RESULT_UNAVAILABLE");
                log.warn(
                        "준비된 검토 결과 불러오기 실패 intakeId={} type={}",
                        id,
                        exception.getClass().getSimpleName());
            }
        }
    }
}
