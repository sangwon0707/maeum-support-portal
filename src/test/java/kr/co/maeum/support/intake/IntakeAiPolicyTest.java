package kr.co.maeum.support.intake;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

class IntakeAiPolicyTest {
    @Test
    @DisplayName("AI 후보는 누락값만 보완하고 기존 값을 보존한다")
    void supplements_missing_values_without_overwriting_existing_fields() {
        var fields = new HashMap<String, Object>(Map.of("businessName", "기존상호"));
        var page = new HashMap<String, Object>();
        page.put("pageNumber", 1);
        page.put("fields", fields);
        page.put("evidence", new HashMap<String, Object>());
        Map<String, Object> candidate =
                Map.of(
                        "pageNumber",
                        1,
                        "fields",
                        Map.of("businessName", "다른상호", "representative", "가상대표"),
                        "evidence",
                        Map.of("representative", Map.of("lineIndex", 2)));
        IntakeAiPolicy.supplement(
                List.of(page), Map.of("status", "SUCCEEDED", "pageResults", List.of(candidate)));
        assertEquals("기존상호", fields.get("businessName"));
        assertEquals("가상대표", fields.get("representative"));
    }

    @Test
    @DisplayName("공급자 실패 시 기존 검토 결과를 유지한다")
    void provider_failure_preserves_existing_review() {
        var page = new HashMap<String, Object>();
        page.put("pageNumber", 1);
        IntakeAiPolicy.supplement(List.of(page), Map.of("status", "TIMEOUT"));
        assertFalse(page.containsKey("ai"));
    }
}
