package kr.co.maeum.support.ocr;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.maeum.support.document.Document;
import kr.co.maeum.support.document.FileStorage;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.file.Files;
import java.time.Duration;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

@Component
public class PaddleOcrProvider {
    private final OcrSettings settings;
    private final FileStorage storage;
    private final OcrFieldExtractor extractor;
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();

    public PaddleOcrProvider(
            OcrSettings settings, FileStorage storage, OcrFieldExtractor extractor) {
        this.settings = settings;
        this.storage = storage;
        this.extractor = extractor;
    }

    public OcrExtraction extract(Document document, boolean assist) {
        if (!settings.liveEnabled() || settings.token().length() < 32) {
            throw new OcrProcessingException("ENGINE_DISABLED", false);
        }
        try {
            var bytes = Files.readAllBytes(storage.resolve(document.getStorageKey()));
            var request =
                    HttpRequest.newBuilder(
                                    URI.create(
                                            settings.endpoint() + "/v1/recognize?assist=" + assist))
                            // AI 보조를 포함해 최대 180초, claim 임대는 240초다.
                            .timeout(Duration.ofSeconds(180))
                            .header("Authorization", "Bearer " + settings.token())
                            .header("Content-Type", "image/png")
                            .POST(HttpRequest.BodyPublishers.ofByteArray(bytes))
                            .build();
            var pending = client.sendAsync(request, info -> new LimitedBodySubscriber());
            HttpResponse<byte[]> response;
            try {
                response = pending.get(180, TimeUnit.SECONDS);
            } catch (TimeoutException exception) {
                pending.cancel(true);
                throw new OcrProcessingException("ENGINE_TIMEOUT", true);
            }
            if (response.statusCode() != 200) {
                int status = response.statusCode();
                throw new OcrProcessingException(
                        "ENGINE_HTTP_" + status, status == 429 || status >= 500);
            }
            byte[] payload = response.body();
            var raw = json.readTree(payload);
            if (!"paddleocr".equals(raw.path("engine").asText())) {
                throw new OcrProcessingException("INVALID_RESPONSE", false);
            }
            if (raw.path("texts").isEmpty()) {
                throw new OcrProcessingException("NO_TEXT", false);
            }
            return extractor.extract(raw);
        } catch (ExecutionException exception) {
            if (exception.getCause() instanceof OcrProcessingException processing) {
                throw processing;
            }
            throw new OcrProcessingException("ENGINE_UNAVAILABLE", true);
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new OcrProcessingException("INTERRUPTED", true);
        } catch (JsonProcessingException exception) {
            throw new OcrProcessingException("INVALID_RESPONSE", false);
        } catch (IOException exception) {
            throw new OcrProcessingException("ENGINE_UNAVAILABLE", true);
        }
    }
}
