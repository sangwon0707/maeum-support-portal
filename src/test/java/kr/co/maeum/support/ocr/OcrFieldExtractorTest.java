package kr.co.maeum.support.ocr;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

class OcrFieldExtractorTest {
    private final ObjectMapper json = new ObjectMapper();
    private final OcrFieldExtractor extractor = new OcrFieldExtractor();

    private Map<String, Object> line(String text, int x, int y) {
        return Map.of(
                "text",
                text,
                "confidence",
                .95,
                "polygon",
                List.of(
                        List.of(x, y),
                        List.of(x + 80, y),
                        List.of(x + 80, y + 20),
                        List.of(x, y + 20)));
    }

    @Test
    @DisplayName("표의 같은 행 오른쪽 값을 추출하고 사업자번호 앞자리 0을 보존한다")
    void table_geometry_extracts_fields_without_application_values() {
        var raw =
                json.valueToTree(
                        Map.of(
                                "texts",
                                List.of(
                                        line("등록번호", 10, 10),
                                        line("000-00-00000", 200, 10),
                                        line("상호", 10, 100),
                                        line("가상상점", 200, 100),
                                        line("개업연월일", 10, 200),
                                        line("2024년 01월 01일", 200, 200))));
        var result = extractor.extract(raw);
        assertEquals("0000000000", result.fields().get("businessNo"));
        assertEquals("가상상점", result.fields().get("businessName"));
        assertEquals("2024-01-01", result.fields().get("openedOn"));
        assertFalse(result.fields().containsKey("representative"));
    }

    @Test
    @DisplayName("AI가 원문에 없는 값을 제안하면 무시한다")
    void ai_hallucination_cannot_fill_missing_field() {
        var raw =
                json.valueToTree(
                        Map.of(
                                "texts",
                                List.of(line("가상상점", 10, 10)),
                                "ai",
                                Map.of(
                                        "status",
                                        "SUCCEEDED",
                                        "fields",
                                        Map.of(
                                                "businessName",
                                                Map.of("value", "없는상호", "lineIndex", 0)))));
        assertTrue(extractor.extract(raw).fields().isEmpty());
    }

    @Test
    @DisplayName("AI와 규칙이 충돌해도 규칙값을 덮어쓰지 않고 확인 필요로 기록한다")
    void ai_conflict_keeps_rule_and_marks_review() {
        var raw =
                json.valueToTree(
                        Map.of(
                                "texts",
                                List.of(line("상호: 가상상점", 10, 10), line("다른상점", 10, 100)),
                                "ai",
                                Map.of(
                                        "status",
                                        "SUCCEEDED",
                                        "fields",
                                        Map.of(
                                                "businessName",
                                                Map.of("value", "다른상점", "lineIndex", 1)))));
        var result = extractor.extract(raw);
        assertEquals("가상상점", result.fields().get("businessName"));
        assertEquals(
                true, ((Map<?, ?>) result.evidence().get("businessName")).get("reviewRequired"));
    }

    @Test
    @DisplayName("잘못된 날짜와 라벨만 있는 문서는 임의로 보완하지 않는다")
    void invalid_date_or_empty_label_is_unextracted() {
        assertNull(OcrFieldExtractor.normalize("openedOn", "2024년 02월 30일"));
        assertTrue(
                extractor
                        .extract(json.valueToTree(Map.of("texts", List.of(line("상호", 10, 10)))))
                        .fields()
                        .isEmpty());
    }
}
