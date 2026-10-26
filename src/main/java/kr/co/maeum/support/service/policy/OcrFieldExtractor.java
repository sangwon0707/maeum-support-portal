package kr.co.maeum.support.service.policy;

import com.fasterxml.jackson.databind.JsonNode;

import kr.co.maeum.support.dto.OcrExtraction;
import kr.co.maeum.support.integration.ocr.OcrProcessingException;

import org.springframework.stereotype.Component;

import java.time.DateTimeException;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.regex.Pattern;

/** 라벨과 같은 줄의 오른쪽 값만 후보로 삼는다. 신청서 값으로 빈칸을 메우지 않는다. */
@Component
public class OcrFieldExtractor {
    private static final Map<String, String> LABELS =
            Map.of(
                    "businessName",
                    "상호|상호명",
                    "businessNo",
                    "등록번호|사업자등록번호|사업자번호|번호",
                    "representative",
                    "성명\\(대표자\\)|대표자|대표자명|성명",
                    "openedOn",
                    "개업연월일|개업일|개업년월일");

    public OcrExtraction extract(JsonNode raw) {
        var lines = raw.path("texts");
        if (!lines.isArray() || lines.size() > 500) {
            throw new OcrProcessingException("INVALID_RESPONSE", false);
        }
        Map<String, String> fields = new LinkedHashMap<>();
        Map<String, Object> evidence = new LinkedHashMap<>();
        for (var field : LABELS.entrySet()) {
            var label = Pattern.compile("^(?:" + field.getValue() + ")[ :：]*");
            for (int i = 0; i < lines.size(); i++) {
                var line = lines.get(i);
                String text = line.path("text").asText("").strip();
                var match = label.matcher(text);
                if (!match.find()) {
                    continue;
                }
                String value = text.substring(match.end()).strip();
                int valueIndex = i;
                // 표 형태는 라벨과 값이 서로 다른 OCR 영역으로 반환된다.
                if (value.isEmpty()) {
                    double closest = Double.MAX_VALUE;
                    for (int j = 0; j < lines.size(); j++) {
                        var candidate = lines.get(j);
                        double dx = x(candidate) - right(line);
                        double dy = Math.abs(y(candidate) - y(line));
                        double height = height(line);
                        if (j != i && dx >= -2 && dy < Math.max(12, height * 0.7) && dx < closest) {
                            value = candidate.path("text").asText("").strip();
                            valueIndex = j;
                            closest = dx;
                        }
                    }
                }
                String normalized = normalize(field.getKey(), value);
                if (normalized == null) {
                    continue;
                }
                // 중복 라벨은 다른 페이지/설명문일 수 있어 자동 선택하지 않는다.
                if (fields.containsKey(field.getKey())) {
                    fields.remove(field.getKey());
                    evidence.remove(field.getKey());
                    break;
                }
                fields.put(field.getKey(), normalized);
                evidence.put(
                        field.getKey(), proof(lines.get(valueIndex), valueIndex, "RULE", value));
            }
        }
        var ai = raw.path("ai");
        if ("SUCCEEDED".equals(ai.path("status").asText())) {
            for (String field : LABELS.keySet()) {
                var candidate = ai.path("fields").path(field);
                int index = candidate.path("lineIndex").asInt(-1);
                String value = candidate.path("value").asText("");
                // 외부 응답은 내부 API가 검증했어도 업무 경계에서 다시 근거를 확인한다.
                if (index < 0
                        || index >= lines.size()
                        || value.isBlank()
                        || !lines.get(index).path("text").asText().contains(value)) {
                    continue;
                }
                String normalized = normalize(field, value);
                if (normalized == null) {
                    continue;
                }
                if (fields.containsKey(field)) {
                    if (!fields.get(field).equals(normalized)) {
                        var conflict = new LinkedHashMap<String, Object>();
                        conflict.put("rule", evidence.get(field));
                        conflict.put("aiCandidate", normalized);
                        conflict.put("reviewRequired", true);
                        evidence.put(field, conflict);
                    }
                } else {
                    fields.put(field, normalized);
                    evidence.put(field, proof(lines.get(index), index, "AI", value));
                }
            }
        }
        return new OcrExtraction(fields, evidence, raw);
    }

    private Map<String, Object> proof(JsonNode line, int index, String source, String value) {
        return Map.of(
                "lineIndex",
                index,
                "source",
                source,
                "originalText",
                value,
                "confidence",
                line.path("confidence").asDouble(0),
                "polygon",
                line.path("polygon"));
    }

    private double x(JsonNode line) {
        return line.path("polygon").path(0).path(0).asDouble(-10000);
    }

    private double right(JsonNode line) {
        return line.path("polygon").path(1).path(0).asDouble(-10000);
    }

    private double y(JsonNode line) {
        return line.path("polygon").path(0).path(1).asDouble(-10000);
    }

    private double height(JsonNode line) {
        return Math.abs(line.path("polygon").path(3).path(1).asDouble() - y(line));
    }

    static String normalize(String field, String value) {
        if (value.isBlank() || value.length() > 200) {
            return null;
        }
        if (field.equals("businessNo")) {
            var number =
                    Pattern.compile("(?<![0-9])([0-9]{3})[- ]?([0-9]{2})[- ]?([0-9]{5})(?![0-9])")
                            .matcher(value);
            return number.matches() ? number.group(1) + number.group(2) + number.group(3) : null;
        }
        if (field.equals("openedOn")) {
            var date =
                    Pattern.compile("([0-9]{4})[년./-]\\s*([0-9]{1,2})[월./-]\\s*([0-9]{1,2})일?")
                            .matcher(value);
            if (!date.matches()) {
                return null;
            }
            try {
                return LocalDate.of(
                                Integer.parseInt(date.group(1)),
                                Integer.parseInt(date.group(2)),
                                Integer.parseInt(date.group(3)))
                        .toString();
            } catch (DateTimeException exception) {
                return null;
            }
        }
        return value.strip();
    }
}
