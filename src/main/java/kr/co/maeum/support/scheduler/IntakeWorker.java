package kr.co.maeum.support.scheduler;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.maeum.support.common.JsonCodec;
import kr.co.maeum.support.integration.ocr.IntakeProvider;
import kr.co.maeum.support.mapper.IntakeMapper;
import kr.co.maeum.support.service.IntakeResultCache;
import kr.co.maeum.support.service.policy.IntakeAiPolicy;
import kr.co.maeum.support.service.policy.IntakeGroupingPolicy;
import kr.co.maeum.support.service.policy.IntakePolicy;
import kr.co.maeum.support.storage.IntakeStorage;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Component
public class IntakeWorker {
    private static final Logger log = LoggerFactory.getLogger(IntakeWorker.class);
    private final kr.co.maeum.support.storage.StorageGuard guard;
    private final IntakeResultCache cache;
    private final IntakeMapper jobs;
    private final IntakeProvider provider;
    private final IntakeStorage storage;
    private final JsonCodec json;
    private final IntakeGroupingPolicy grouping;
    private final ObjectMapper mapper = new ObjectMapper();

    public IntakeWorker(
            IntakeMapper jobs,
            IntakeProvider provider,
            IntakeStorage storage,
            JsonCodec json,
            IntakeGroupingPolicy grouping,
            kr.co.maeum.support.storage.StorageGuard guard,
            IntakeResultCache cache) {
        this.guard = guard;
        this.cache = cache;
        this.jobs = jobs;
        this.provider = provider;
        this.storage = storage;
        this.json = json;
        this.grouping = grouping;
    }

    @Scheduled(fixedDelay = 1500)
    public void runPending() {
        try (var ignored = guard.use()) {
            processPending();
        } catch (Exception exception) {
            log.warn("문서 처리 실행 실패 type={}", exception.getClass().getSimpleName());
        }
    }

    private void processPending() {
        jobs.expireInterrupted();
        for (long id : jobs.pending()) {
            String token = UUID.randomUUID().toString();
            if (jobs.claim(id, token) != 1) continue;
            var generated = new ArrayList<String>();
            try {
                var pages = new ArrayList<Map<String, Object>>();
                var aiAnalyses = new ArrayList<Map<String, Object>>();
                var sources = jobs.sources(id);
                for (int index = 0; index < sources.size(); index++) {
                    var source = sources.get(index);
                    final int fileIndex = index;
                    provider.analyze(
                            storage.resolve(source.get("storage_key").toString()),
                            source.get("kind").toString(),
                            event -> {
                                String eventType = event.path("event").asText();
                                if ("ai".equals(eventType)) {
                                    Map<String, Object> analysis =
                                            mapper.convertValue(
                                                    event,
                                                    new TypeReference<Map<String, Object>>() {});
                                    analysis.remove("event");
                                    analysis.put("sourceName", source.get("original_name"));
                                    @SuppressWarnings("unchecked")
                                    var candidates =
                                            (List<Map<String, Object>>) analysis.get("pageResults");
                                    for (var candidate : candidates) {
                                        if (candidate.containsKey("suggestedKind")) {
                                            candidate.put(
                                                    "kindLabel",
                                                    IntakePolicy.label(
                                                            candidate
                                                                    .get("suggestedKind")
                                                                    .toString()));
                                        }
                                    }
                                    var sourcePages =
                                            pages.stream()
                                                    .filter(
                                                            page ->
                                                                    page.get("sourceId")
                                                                            .equals(
                                                                                    source.get(
                                                                                            "id")))
                                                    .toList();
                                    IntakeAiPolicy.supplement(sourcePages, analysis);
                                    aiAnalyses.add(analysis);
                                }
                                if ("stage".equals(eventType)) {
                                    int progress =
                                            10
                                                    + (int)
                                                            (80.0
                                                                    * (fileIndex
                                                                            + (event.path(
                                                                                                            "pageNumber")
                                                                                                    .asInt()
                                                                                            - 1.0)
                                                                                    / Math.max(
                                                                                            1,
                                                                                            event.path(
                                                                                                            "pageCount")
                                                                                                    .asInt()))
                                                                    / sources.size());
                                    if ("AI_REVIEW".equals(event.path("stage").asText())) {
                                        progress = 93;
                                    }
                                    if (jobs.progress(
                                                    id,
                                                    token,
                                                    event.path("stage").asText(),
                                                    progress,
                                                    source.get("original_name")
                                                            + " · "
                                                            + event.path("pageNumber").asInt()
                                                            + "/"
                                                            + event.path("pageCount").asInt()
                                                            + "쪽")
                                            != 1) throw new IllegalStateException("CLAIM_LOST");
                                }
                                if ("page".equals(eventType)) {
                                    if (pages.size() >= 12)
                                        throw new IllegalStateException("TOO_MANY_PAGES");
                                    var page =
                                            mapper.convertValue(
                                                    event,
                                                    new TypeReference<Map<String, Object>>() {});
                                    page.remove("event");
                                    page.put("sourceId", source.get("id"));
                                    page.put("sourceName", source.get("original_name"));
                                    page.put(
                                            "previewKey",
                                            saveImage(event.path("preview"), generated));
                                    page.remove("preview");
                                    var photos = new ArrayList<Map<String, Object>>();
                                    for (var photo : event.path("photos")) {
                                        var item =
                                                mapper.convertValue(
                                                        photo,
                                                        new TypeReference<
                                                                Map<String, Object>>() {});
                                        item.remove("image");
                                        item.put(
                                                "storageKey",
                                                saveImage(photo.path("image"), generated));
                                        photos.add(item);
                                    }
                                    page.put("photos", photos);
                                    page.put(
                                            "kindLabel",
                                            IntakePolicy.label(event.path("kind").asText()));
                                    pages.add(page);
                                    int progress =
                                            10
                                                    + (int)
                                                            (80.0
                                                                    * (fileIndex
                                                                            + (double)
                                                                                            event.path(
                                                                                                            "pageNumber")
                                                                                                    .asInt()
                                                                                    / event.path(
                                                                                                    "pageCount")
                                                                                            .asInt())
                                                                    / sources.size());
                                    jobs.progress(
                                            id,
                                            token,
                                            "READING",
                                            progress,
                                            source.get("original_name")
                                                    + " · "
                                                    + event.path("pageNumber").asInt()
                                                    + "쪽 분석 완료");
                                }
                            });
                }
                jobs.progress(id, token, "ASSEMBLING", 95, "문서별 PDF와 추출 항목 정리 중");
                var result = new LinkedHashMap<String, Object>(assemble(pages));
                result.put("aiAnalyses", aiAnalyses);
                @SuppressWarnings("unchecked")
                var conflicts = (List<Map<String, Object>>) result.get("conflicts");
                for (var analysis : aiAnalyses) {
                    @SuppressWarnings("unchecked")
                    var differences = (List<Map<String, Object>>) analysis.get("conflicts");
                    for (var difference : differences) {
                        difference.put("sourceName", analysis.get("sourceName"));
                        difference.put("method", "LLM_SOURCE_VERIFIED");
                        conflicts.add(difference);
                    }
                }
                var groups =
                        grouping.group(
                                pages,
                                pages.stream().map(page -> page.get("kind").toString()).toList(),
                                List.of());
                for (var group : groups) {
                    var source =
                            sources.stream()
                                    .filter(
                                            item ->
                                                    item.get("id")
                                                            .toString()
                                                            .equals(
                                                                    group.get("sourceId")
                                                                            .toString()))
                                    .findFirst()
                                    .orElseThrow();
                    @SuppressWarnings("unchecked")
                    var numbers = (List<Integer>) group.get("pageNumbers");
                    String key =
                            storage.save(
                                    provider.split(
                                            storage.resolve(source.get("storage_key").toString()),
                                            numbers),
                                    true);
                    generated.add(key);
                    group.put("pdfKey", key);
                }
                result.put("groups", groups);
                if (jobs.finish(id, token, json.write(result)) != 1) {
                    generated.forEach(storage::delete);
                } else {
                    cache.remember(id, sources, result);
                }
            } catch (RuntimeException exception) {
                generated.forEach(storage::delete);
                jobs.fail(
                        id,
                        token,
                        exception instanceof kr.co.maeum.support.common.BusinessException business
                                        && business.getStatus() == 507
                                ? "STORAGE_CAPACITY_REACHED"
                                : "DOCUMENT_ANALYSIS_FAILED");
                log.warn("문서 분석 실패 intakeId={} type={}", id, exception.getClass().getSimpleName());
            }
        }
    }

    private String saveImage(JsonNode image, List<String> generated) {
        byte[] bytes = Base64.getDecoder().decode(image.asText());
        if (bytes.length == 0 || bytes.length > 12 * 1024 * 1024)
            throw new IllegalStateException("INVALID_DERIVED_IMAGE");
        String key = storage.save(bytes, false);
        generated.add(key);
        return key;
    }

    @SuppressWarnings("unchecked")
    private Map<String, Object> assemble(List<Map<String, Object>> pages) {
        var fields = new LinkedHashMap<String, Object>();
        var evidence = new LinkedHashMap<String, Object>();
        var conflicts = new ArrayList<Map<String, Object>>();
        int photoCount = 0;
        for (var page : pages) {
            var extracted = (Map<String, Object>) page.get("fields");
            for (var field : extracted.entrySet()) {
                if ("planText".equals(field.getKey()) && fields.containsKey("planText")) {
                    String combined = fields.get("planText") + "\n" + field.getValue();
                    fields.put(
                            "planText", combined.substring(0, Math.min(2000, combined.length())));
                    continue;
                }
                if (fields.containsKey(field.getKey())
                        && !fields.get(field.getKey()).equals(field.getValue())) {
                    conflicts.add(
                            Map.of(
                                    "field",
                                    field.getKey(),
                                    "previous",
                                    fields.get(field.getKey()),
                                    "candidate",
                                    field.getValue(),
                                    "pageNumber",
                                    page.get("pageNumber"),
                                    "sourceName",
                                    page.get("sourceName")));
                    continue;
                }
                fields.put(field.getKey(), field.getValue());
                evidence.put(
                        field.getKey(),
                        Map.of(
                                "pageNumber",
                                page.get("pageNumber"),
                                "sourceName",
                                page.get("sourceName"),
                                "sourceId",
                                page.get("sourceId"),
                                "textSource",
                                page.get("textSource")));
            }
            photoCount += ((List<?>) page.get("photos")).size();
        }
        return Map.of(
                "pages",
                pages,
                "fields",
                fields,
                "evidence",
                evidence,
                "conflicts",
                conflicts,
                "pageCount",
                pages.size(),
                "photoCount",
                photoCount);
    }
}
