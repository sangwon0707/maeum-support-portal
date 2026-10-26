package kr.co.maeum.support.intake;

import java.util.List;
import java.util.Map;

/** LLM 후보는 원문 검증 결과만 보완하고 기존 값과의 차이는 검토 대상으로 남긴다. */
public final class IntakeAiPolicy {
    private IntakeAiPolicy() {}

    @SuppressWarnings("unchecked")
    public static void supplement(List<Map<String, Object>> pages, Map<String, Object> analysis) {
        if (!"SUCCEEDED".equals(analysis.get("status"))) {
            return;
        }
        for (var candidate : (List<Map<String, Object>>) analysis.get("pageResults")) {
            var page =
                    pages.stream()
                            .filter(
                                    item ->
                                            item.get("pageNumber")
                                                    .equals(candidate.get("pageNumber")))
                            .findFirst()
                            .orElseThrow();
            var fields = (Map<String, Object>) page.get("fields");
            var evidence = (Map<String, Object>) page.get("evidence");
            var candidateEvidence = (Map<String, Object>) candidate.get("evidence");
            var candidates = (Map<String, Object>) candidate.get("fields");
            // 기존 값은 사용자 확인 전 바꾸지 않는다. 누락 항목만 원문 근거와 함께 보완한다.
            for (var entry : candidates.entrySet()) {
                if (!fields.containsKey(entry.getKey())) {
                    fields.put(entry.getKey(), entry.getValue());
                    evidence.put(entry.getKey(), candidateEvidence.get(entry.getKey()));
                }
            }
            page.put("ai", candidate);
        }
    }
}
