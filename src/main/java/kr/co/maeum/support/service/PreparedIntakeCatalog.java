package kr.co.maeum.support.service;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.node.ObjectNode;

import kr.co.maeum.support.storage.IntakeStorage;

import org.springframework.stereotype.Component;

import java.io.IOException;
import java.security.MessageDigest;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;

/** 원본 바이트가 일치하는 제공 문서에만 사전 실인식 산출물을 연결한다. */
@Component
public class PreparedIntakeCatalog {
    private final ObjectMapper json = new ObjectMapper();
    private final Map<String, String> cases = new HashMap<>();
    private final IntakeStorage storage;

    public PreparedIntakeCatalog(IntakeStorage storage) throws IOException {
        this.storage = storage;
        try (var stream = getClass().getResourceAsStream("/prepared-intakes/index.json")) {
            if (stream != null)
                for (var item : json.readTree(stream).path("cases"))
                    cases.put(item.path("sourceSha256").asText(), item.path("code").asText());
        }
    }

    public String match(String layout, List<Map<String, Object>> sources) {
        if (!"BUNDLE".equals(layout)
                || sources.size() != 1
                || !"AUTO".equals(sources.getFirst().get("kind"))) return null;
        try {
            byte[] bytes =
                    java.nio.file.Files.readAllBytes(
                            storage.resolve(sources.getFirst().get("storage_key").toString()));
            return cases.get(
                    HexFormat.of().formatHex(MessageDigest.getInstance("SHA-256").digest(bytes)));
        } catch (IOException | java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("준비된 문서 식별에 실패했습니다.", exception);
        }
    }

    public JsonNode materialize(String code, Map<String, Object> source) {
        try (var stream = getClass().getResourceAsStream("/prepared-intakes/" + code + ".json")) {
            if (stream == null) throw new IllegalStateException("준비된 결과가 없습니다.");
            var manifest = json.readTree(stream);
            var result = manifest.path("result");
            restore(result, source);
            ((ObjectNode) result).put("preparedAt", manifest.path("preparedAt").asText());
            ((ObjectNode) result).put("executionMode", "PREPARED");
            return result;
        } catch (IOException exception) {
            throw new IllegalStateException("준비된 결과를 읽지 못했습니다.", exception);
        }
    }

    private void restore(JsonNode node, Map<String, Object> source) throws IOException {
        if (node.isObject()) {
            var object = (ObjectNode) node;
            if (object.has("sourceId"))
                object.put("sourceId", ((Number) source.get("id")).longValue());
            if (object.has("sourceName"))
                object.put("sourceName", source.get("original_name").toString());
            for (String field : List.of("previewKey", "storageKey", "pdfKey")) {
                if (!object.has(field)) continue;
                String key = object.path(field).asText();
                if (!key.matches("[a-f0-9]{64}\\.(pdf|png)"))
                    throw new IllegalStateException("준비된 파일 키가 올바르지 않습니다.");
                try (var asset =
                        getClass().getResourceAsStream("/prepared-intakes/assets/" + key)) {
                    if (asset == null
                            || !key.equals(
                                    storage.save(asset.readAllBytes(), key.endsWith(".pdf"))))
                        throw new IllegalStateException("준비된 파일 검증에 실패했습니다.");
                }
            }
        }
        for (JsonNode child : node) restore(child, source);
    }
}
