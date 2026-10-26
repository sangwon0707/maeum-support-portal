package kr.co.maeum.support.ocr;

import static org.junit.jupiter.api.Assertions.*;

import com.sun.net.httpserver.HttpServer;

import kr.co.maeum.support.document.Document;
import kr.co.maeum.support.document.FileStorage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;

class PaddleOcrProviderTest {
    @TempDir Path directory;

    private OcrExtraction request(int status, String payload) throws Exception {
        var server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(
                "/v1/recognize",
                exchange -> {
                    assertEquals(
                            "Bearer " + "a".repeat(32),
                            exchange.getRequestHeaders().getFirst("Authorization"));
                    assertEquals("assist=false", exchange.getRequestURI().getQuery());
                    exchange.getRequestBody().readAllBytes();
                    byte[] bytes = payload.getBytes(StandardCharsets.UTF_8);
                    exchange.sendResponseHeaders(status, bytes.length);
                    try (var output = exchange.getResponseBody()) {
                        output.write(bytes);
                    }
                });
        server.start();
        Path image = directory.resolve("input.png");
        Files.write(image, new byte[] {1, 2, 3});
        var settings =
                new OcrSettings() {
                    public boolean liveEnabled() {
                        return true;
                    }

                    public String token() {
                        return "a".repeat(32);
                    }

                    public String endpoint() {
                        return "http://127.0.0.1:" + server.getAddress().getPort();
                    }
                };
        FileStorage storage =
                new FileStorage() {
                    public Path resolve(String key) {
                        return image;
                    }

                    public String save(byte[] bytes) {
                        throw new UnsupportedOperationException();
                    }

                    public void delete(String key) {
                        throw new UnsupportedOperationException();
                    }
                };
        try {
            return new PaddleOcrProvider(settings, storage, new OcrFieldExtractor())
                    .extract(new Document(), false);
        } finally {
            server.stop(0);
        }
    }

    @Test
    @DisplayName("내부 인증을 전달하고 인식 응답에서 항목 후보를 추출한다")
    void actual_http_contract_extracts_value() throws Exception {
        assertEquals(
                "가상상점",
                request(200, "{\"engine\":\"paddleocr\",\"texts\":[{\"text\":\"상호: 가상상점\"}]}")
                        .fields()
                        .get("businessName"));
    }

    @Test
    @DisplayName("일시적 엔진 장애만 재시도 대상으로 분류한다")
    void http_error_classifies_retry_without_exposing_body() {
        var busy = assertThrows(OcrProcessingException.class, () -> request(429, "private detail"));
        assertTrue(busy.retryable());
        assertEquals("ENGINE_HTTP_429", busy.code());
        var auth = assertThrows(OcrProcessingException.class, () -> request(401, "private detail"));
        assertFalse(auth.retryable());
        assertFalse(auth.getMessage().contains("private"));
    }

    @Test
    @DisplayName("깨진 응답과 빈 인식은 성공으로 저장하지 않는다")
    void invalid_json_and_blank_result_fail_explicitly() {
        assertEquals(
                "INVALID_RESPONSE",
                assertThrows(OcrProcessingException.class, () -> request(200, "broken")).code());
        assertEquals(
                "NO_TEXT",
                assertThrows(
                                OcrProcessingException.class,
                                () -> request(200, "{\"engine\":\"paddleocr\",\"texts\":[]}"))
                        .code());
    }

    @Test
    @DisplayName("1MiB 초과 응답을 수신 중에 차단한다")
    void excessive_response_is_cancelled() {
        var error =
                assertThrows(
                        OcrProcessingException.class,
                        () -> request(200, "a".repeat(1024 * 1024 + 1)));
        assertEquals("RESPONSE_TOO_LARGE", error.code());
        assertFalse(error.retryable());
    }
}
