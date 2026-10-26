package kr.co.maeum.support.intake;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import kr.co.maeum.support.config.DatabaseConfig;

import org.springframework.stereotype.Component;

import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

/** 사용자 확인값과 분리한 실제 엔진 결과만 동일 소유자·동일 처리 버전에서 재사용한다. */
@Component
public class IntakeResultCache {
    private final IntakeMapper jobs;
    private final IntakeStorage storage;
    private final ObjectMapper json = new ObjectMapper();
    private static final Pattern KEY =
            Pattern.compile("(?:[a-f0-9-]{36}|[a-f0-9]{64})\\.(?:pdf|png)");

    public IntakeResultCache(IntakeMapper jobs, IntakeStorage storage) {
        this.jobs = jobs;
        this.storage = storage;
    }

    private String version() {
        return DatabaseConfig.env(
                "DOCUMENT_PIPELINE_VERSION", "pdfbox-3.0.8-paddle-v5-ko-layout-v2");
    }

    public String reuse(long id, List<Map<String, Object>> sources) {
        var scope = jobs.cacheScope(id);
        if (scope == null || scope.get("request_hash") == null) return null;
        String payload = jobs.cached(scope, version());
        if (payload == null) return null;
        try {
            var matches = KEY.matcher(payload);
            while (matches.find())
                if (!Files.isRegularFile(storage.resolve(matches.group()))) return null;
            var cached = json.readTree(payload);
            if (!cached.path("result").isObject()
                    || cached.path("sources").size() != sources.size()) return null;
            Map<String, Map<String, Object>> replacements = new HashMap<>();
            for (int index = 0; index < sources.size(); index++) {
                replacements.put(
                        cached.path("sources").get(index).path("id").asText(), sources.get(index));
            }
            JsonNode result = cached.path("result");
            rebind(result, replacements);
            return json.writeValueAsString(result);
        } catch (java.io.IOException | IllegalStateException exception) {
            return null;
        }
    }

    private void rebind(JsonNode node, Map<String, Map<String, Object>> replacements) {
        if (node.isObject() && node.has("sourceId")) {
            var source = replacements.get(node.path("sourceId").asText());
            if (source == null) throw new IllegalStateException("캐시 문서 연결이 올바르지 않습니다.");
            ((ObjectNode) node).put("sourceId", ((Number) source.get("id")).longValue());
            if (node.has("sourceName"))
                ((ObjectNode) node).put("sourceName", source.get("original_name").toString());
        }
        node.forEach(child -> rebind(child, replacements));
    }

    public void remember(long id, List<Map<String, Object>> sources, Map<String, Object> result) {
        var scope = jobs.cacheScope(id);
        if (scope == null || scope.get("request_hash") == null) return;
        try {
            jobs.cache(
                    scope,
                    version(),
                    json.writeValueAsString(Map.of("sources", sources, "result", result)));
        } catch (java.io.IOException exception) {
            throw new IllegalStateException("인식 결과 재사용 정보를 저장하지 못했습니다.", exception);
        }
    }
}
