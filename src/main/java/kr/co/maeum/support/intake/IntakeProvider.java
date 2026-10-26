package kr.co.maeum.support.intake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.maeum.support.ocr.OcrSettings;

import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.nio.charset.StandardCharsets;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.Consumer;

@Component
public class IntakeProvider {
    private final OcrSettings settings;
    private final HttpClient client =
            HttpClient.newBuilder().connectTimeout(Duration.ofSeconds(5)).build();
    private final ObjectMapper json = new ObjectMapper();

    public IntakeProvider(OcrSettings settings) {
        this.settings = settings;
    }

    public byte[] split(Path source, java.util.List<Integer> pages) {
        try {
            String selected =
                    pages.stream()
                            .map(String::valueOf)
                            .collect(java.util.stream.Collectors.joining(","));
            var request =
                    HttpRequest.newBuilder(
                                    URI.create(
                                            settings.endpoint()
                                                    + "/v1/split-document?pages="
                                                    + selected))
                            .header("Authorization", "Bearer " + settings.token())
                            .timeout(Duration.ofSeconds(30))
                            .POST(HttpRequest.BodyPublishers.ofFile(source))
                            .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofByteArray());
            if (response.statusCode() != 200 || response.body().length > 20 * 1024 * 1024)
                throw new IllegalStateException("DOCUMENT_SPLIT_FAILED");
            return response.body();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DOCUMENT_SPLIT_INTERRUPTED", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("DOCUMENT_SPLIT_FAILED", exception);
        }
    }

    public void analyze(Path source, String kind, Consumer<JsonNode> onEvent) {
        if (!settings.liveEnabled()) throw new IllegalStateException("DOCUMENT_ENGINE_DISABLED");
        try {
            var request =
                    HttpRequest.newBuilder(
                                    URI.create(
                                            settings.endpoint()
                                                    + "/v1/analyze-document?kind="
                                                    + kind
                                                    + "&assist="
                                                    + settings.aiEnabled()))
                            .header("Authorization", "Bearer " + settings.token())
                            .header("Content-Type", "application/octet-stream")
                            .timeout(Duration.ofSeconds(20))
                            .POST(HttpRequest.BodyPublishers.ofFile(source))
                            .build();
            var response = client.send(request, HttpResponse.BodyHandlers.ofInputStream());
            try (var body = response.body()) {
                if (response.statusCode() != 200)
                    throw new IllegalStateException("DOCUMENT_ENGINE_" + response.statusCode());
                // 페이지 단위 이벤트를 읽되 공급자 정지 시 작업 스레드가 영구 점유되지 않게 한다.
                var reading =
                        CompletableFuture.runAsync(
                                () -> {
                                    try (var reader =
                                            new BufferedReader(
                                                    new InputStreamReader(
                                                            body, StandardCharsets.UTF_8))) {
                                        boolean complete = false;
                                        for (String line; (line = reader.readLine()) != null; ) {
                                            if (line.length() > 20 * 1024 * 1024)
                                                throw new IllegalStateException(
                                                        "DOCUMENT_RESPONSE_TOO_LARGE");
                                            var event = json.readTree(line);
                                            if ("error".equals(event.path("event").asText()))
                                                throw new IllegalStateException(
                                                        event.path("code").asText());
                                            onEvent.accept(event);
                                            if ("complete".equals(event.path("event").asText()))
                                                complete = true;
                                        }
                                        if (!complete)
                                            throw new IllegalStateException(
                                                    "DOCUMENT_RESPONSE_INTERRUPTED");
                                    } catch (Exception exception) {
                                        throw new IllegalStateException(
                                                "DOCUMENT_RESPONSE_FAILED", exception);
                                    }
                                });
                try {
                    reading.get(settings.documentTimeoutSeconds(), TimeUnit.SECONDS);
                } finally {
                    reading.cancel(true);
                }
            }
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException("DOCUMENT_INTERRUPTED", exception);
        } catch (Exception exception) {
            throw new IllegalStateException("DOCUMENT_ENGINE_UNAVAILABLE", exception);
        }
    }
}
