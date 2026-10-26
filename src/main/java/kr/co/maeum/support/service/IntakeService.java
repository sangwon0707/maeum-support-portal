package kr.co.maeum.support.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.common.JsonCodec;
import kr.co.maeum.support.config.OcrSettings;
import kr.co.maeum.support.dto.ApplicationForm;
import kr.co.maeum.support.integration.ocr.IntakeProvider;
import kr.co.maeum.support.mapper.ApplicationMapper;
import kr.co.maeum.support.mapper.DocumentMapper;
import kr.co.maeum.support.mapper.IntakeMapper;
import kr.co.maeum.support.model.Application;
import kr.co.maeum.support.security.AccessScope;
import kr.co.maeum.support.security.PortalPrincipal;
import kr.co.maeum.support.service.policy.ImageValidator;
import kr.co.maeum.support.service.policy.IntakeGroupingPolicy;
import kr.co.maeum.support.service.policy.IntakePolicy;
import kr.co.maeum.support.storage.IntakeStorage;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.io.IOException;
import java.math.BigDecimal;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
public class IntakeService {
    private static final org.slf4j.Logger log =
            org.slf4j.LoggerFactory.getLogger(IntakeService.class);

    public record Upload(String name, String kind, byte[] content) {}

    private final PreparedIntakeCatalog prepared;
    private final IntakeMapper jobs;
    private final ApplicationMapper applications;
    private final ApplicationService workflow;
    private final DocumentService documents;
    private final DocumentMapper documentMapper;
    private final OcrService ocr;
    private final AccessScope access;
    private final IntakeStorage storage;
    private final ImageValidator images;
    private final IntakePolicy policy;
    private final IntakeGroupingPolicy grouping;
    private final IntakeProvider provider;
    private final OcrSettings settings;
    private final AuditService audit;
    private final JsonCodec json;
    private final ObjectMapper mapper = new ObjectMapper();
    private final TransactionTemplate transaction;

    public IntakeService(
            IntakeMapper jobs,
            ApplicationMapper applications,
            ApplicationService workflow,
            DocumentService documents,
            DocumentMapper documentMapper,
            OcrService ocr,
            AccessScope access,
            IntakeStorage storage,
            ImageValidator images,
            IntakePolicy policy,
            IntakeGroupingPolicy grouping,
            IntakeProvider provider,
            OcrSettings settings,
            AuditService audit,
            JsonCodec json,
            PlatformTransactionManager manager,
            PreparedIntakeCatalog prepared) {
        this.prepared = prepared;
        this.jobs = jobs;
        this.applications = applications;
        this.workflow = workflow;
        this.documents = documents;
        this.documentMapper = documentMapper;
        this.ocr = ocr;
        this.access = access;
        this.storage = storage;
        this.images = images;
        this.policy = policy;
        this.grouping = grouping;
        this.provider = provider;
        this.settings = settings;
        this.audit = audit;
        this.json = json;
        this.transaction = new TransactionTemplate(manager);
    }

    @Transactional
    public long upload(
            PortalPrincipal actor,
            long applicationId,
            String layout,
            List<Upload> uploads,
            String requestKey) {
        var application = applications.lock(applicationId);
        editable(actor, application);
        policy.files(layout, uploads.size(), uploads.stream().map(Upload::kind).toList());
        String key = requestKey == null ? java.util.UUID.randomUUID().toString() : requestKey;
        if (!key.matches(
                "[a-fA-F0-9]{8}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{4}-[a-fA-F0-9]{12}")) {
            throw new BusinessException(422, "접수 식별자가 올바르지 않습니다. 파일을 다시 선택해 주세요.");
        }
        String hash = requestHash(layout, uploads);
        var previous = jobs.byRequest(applicationId, key);
        if (previous != null) {
            if (!hash.equals(previous.get("request_hash"))) {
                throw new BusinessException(409, "접수하려는 파일이 변경됐습니다. 파일을 다시 선택해 주세요.");
            }
            // 응답 유실·중복 클릭이어도 이미 접수한 작업을 다시 만들지 않는다.
            log.info("중복 접수 재사용 applicationId={} intakeId={}", applicationId, previous.get("id"));
            return ((Number) previous.get("id")).longValue();
        }
        var job = new HashMap<String, Object>();
        job.put("applicationId", applicationId);
        job.put("createdBy", actor.id());
        job.put("layout", layout);
        job.put("requestKey", key);
        job.put("requestHash", hash);
        jobs.insert(job);
        long id = ((Number) job.get("id")).longValue();
        for (var upload : uploads) {
            boolean pdf = policy.isPdf(upload.content());
            byte[] bytes = pdf ? upload.content() : images.normalize(upload.content());
            String storageKey = storage.save(bytes, pdf);
            rollbackFile(storageKey);
            var source = new HashMap<String, Object>();
            source.put("intakeId", id);
            source.put("kind", upload.kind());
            source.put("originalName", cleanName(upload.name()));
            source.put("storageKey", storageKey);
            source.put("mimeType", pdf ? "application/pdf" : "image/png");
            source.put("sizeBytes", bytes.length);
            jobs.source(source);
        }
        audit.record(
                actor,
                applicationId,
                "DOCUMENT_INTAKE_CREATED",
                "INTAKE",
                id,
                "문서 업로드 완료 · 분석은 확인 후 시작");
        return id;
    }

    @Transactional
    public void startAnalysis(PortalPrincipal actor, long applicationId, long intakeId) {
        startAnalysis(actor, applicationId, intakeId, "LIVE", true);
    }

    public boolean preparedAvailable(Map<String, Object> job) {
        return job != null
                && prepared.match(
                                job.get("layout").toString(),
                                jobs.sources(((Number) job.get("id")).longValue()))
                        != null;
    }

    @Transactional
    public void startAnalysis(
            PortalPrincipal actor,
            long applicationId,
            long intakeId,
            String mode,
            boolean slowAcknowledged) {
        var application = applications.lock(applicationId);
        editable(actor, application);
        var job = jobs.lock(intakeId);
        if (job == null || ((Number) job.get("application_id")).longValue() != applicationId) {
            throw BusinessException.forbidden();
        }
        // 새로고침·중복 POST로 같은 문서의 분석을 다시 실행하지 않는다.
        if (List.of("QUEUED", "PROCESSING", "READY", "APPLIED").contains(job.get("status"))) {
            return;
        }
        if (!"UPLOADED".equals(job.get("status"))) {
            throw new BusinessException(409, "파일을 다시 업로드한 뒤 분석을 시작해 주세요.");
        }
        if (!List.of("PREPARED", "LIVE").contains(mode))
            throw new BusinessException(422, "검토 방식을 선택해 주세요.");
        if ("PREPARED".equals(mode) && !preparedAvailable(job))
            throw new BusinessException(422, "이 파일의 준비된 결과가 없습니다. 실제 OCR 검토를 선택해 주세요.");
        if ("LIVE".equals(mode) && !slowAcknowledged)
            throw new BusinessException(422, "실제 OCR 처리 시간이 길 수 있다는 안내를 확인해 주세요.");
        if ("LIVE".equals(mode) && !settings.liveEnabled())
            throw new BusinessException(503, "문서 처리 서버가 연결되지 않았습니다.");
        var active =
                jobs.activeForProgram(actor.workspaceId(), actor.id(), application.getProgramId());
        if (active != null) {
            throw new BusinessException(409, "이 신청의 다른 문서를 분석하고 있습니다. 완료 후 시작해 주세요.");
        }
        if (("PREPARED".equals(mode) ? jobs.beginPrepared(intakeId) : jobs.enqueue(intakeId))
                != 1) {
            throw BusinessException.conflict();
        }
        audit.record(
                actor,
                applicationId,
                "DOCUMENT_ANALYSIS_STARTED",
                "INTAKE",
                intakeId,
                "원본 확인 후 검토 시작 · " + mode);
        log.info("사용자 확인 후 분석 시작 applicationId={} intakeId={}", applicationId, intakeId);
    }

    @Transactional
    public long requestLiveTest(
            PortalPrincipal actor,
            long applicationId,
            long intakeId,
            String requestKey,
            boolean acknowledged) {
        var application = applications.lock(applicationId);
        editable(actor, application);
        if (!acknowledged) throw new BusinessException(422, "실제 OCR 처리 시간 안내를 확인해 주세요.");
        var original = view(actor, applicationId, intakeId);
        if (original == null
                || !"PREPARED".equals(original.get("execution_mode"))
                || !"READY".equals(original.get("status"))) throw BusinessException.conflict();
        var uploads = new java.util.ArrayList<Upload>();
        for (var source : jobs.sources(intakeId)) {
            try {
                uploads.add(
                        new Upload(
                                source.get("original_name").toString(),
                                source.get("kind").toString(),
                                java.nio.file.Files.readAllBytes(
                                        storage.resolve(source.get("storage_key").toString()))));
            } catch (IOException exception) {
                throw new IllegalStateException("원본을 읽지 못했습니다.", exception);
            }
        }
        long next =
                upload(
                        actor,
                        applicationId,
                        original.get("layout").toString(),
                        uploads,
                        requestKey);
        startAnalysis(actor, applicationId, next, "LIVE", true);
        return next;
    }

    public Map<String, Object> activeForProgram(PortalPrincipal actor, long programId) {
        access.role(actor, "APPLICANT");
        var job = jobs.activeForProgram(actor.workspaceId(), actor.id(), programId);
        if (job == null) {
            return Map.of("active", false);
        }
        long applicationId = ((Number) job.get("application_id")).longValue();
        return Map.of(
                "active",
                true,
                "status",
                job.get("status"),
                "progress",
                job.get("progress"),
                "resumeUrl",
                "/applications/" + applicationId + "/intake?intakeId=" + job.get("id"));
    }

    private String requestHash(String layout, List<Upload> uploads) {
        try {
            var digest = java.security.MessageDigest.getInstance("SHA-256");
            digest.update(layout.getBytes(java.nio.charset.StandardCharsets.UTF_8));
            for (var upload : uploads) {
                policy.isPdf(upload.content());
                digest.update(upload.kind().getBytes(java.nio.charset.StandardCharsets.UTF_8));
                digest.update(
                        java.nio.ByteBuffer.allocate(4).putInt(upload.content().length).array());
                digest.update(upload.content());
            }
            return java.util.HexFormat.of().formatHex(digest.digest());
        } catch (java.security.NoSuchAlgorithmException exception) {
            throw new IllegalStateException("접수 파일 식별값을 만들지 못했습니다.", exception);
        }
    }

    private String cleanName(String name) {
        String value =
                name == null ? "신청문서" : name.replaceAll("[\\r\\n\\u0000]", "").replace('\\', '/');
        value = value.substring(value.lastIndexOf('/') + 1);
        return value.substring(0, Math.min(value.length(), 255));
    }

    private void rollbackFile(String key) {
        TransactionSynchronizationManager.registerSynchronization(
                new TransactionSynchronization() {
                    @Override
                    public void afterCompletion(int status) {
                        if (status != STATUS_COMMITTED) storage.delete(key);
                    }
                });
    }

    private void editable(PortalPrincipal actor, Application application) {
        access.owner(actor, application);
        if (actor.demo() || !"DRAFT".equals(application.getStatus()))
            throw new BusinessException(409, "작성 중인 신청서에서만 문서를 분석·반영할 수 있습니다.");
    }

    public Map<String, Object> view(PortalPrincipal actor, long applicationId, Long intakeId) {
        workflow.view(actor, applicationId);
        var job = intakeId == null ? jobs.latest(applicationId) : jobs.find(intakeId);
        if (job != null && ((Number) job.get("application_id")).longValue() != applicationId)
            throw BusinessException.forbidden();
        return job;
    }

    public Map<String, Object> result(Map<String, Object> job) {
        if (job == null || job.get("result_json") == null) return Map.of();
        try {
            return mapper.readValue(
                    job.get("result_json").toString(),
                    new TypeReference<LinkedHashMap<String, Object>>() {});
        } catch (IOException exception) {
            throw new IllegalStateException("분석 결과를 읽지 못했습니다.", exception);
        }
    }

    public List<Map<String, Object>> sources(long id) {
        return jobs.sources(id);
    }

    @SuppressWarnings("unchecked")
    public ApplicationForm form(
            Application application, Map<String, Object> result, boolean applied) {
        var form = new ApplicationForm();
        BeanUtils.copyProperties(application, form);
        if (applied || !result.containsKey("fields")) return form;
        var values = (Map<String, Object>) result.get("fields");
        if (values.containsKey("businessName"))
            form.setBusinessName(values.get("businessName").toString());
        if (values.containsKey("businessNo"))
            form.setBusinessNo(values.get("businessNo").toString());
        if (values.containsKey("representative"))
            form.setRepresentative(values.get("representative").toString());
        if (values.containsKey("phone")) form.setPhone(values.get("phone").toString());
        if (values.containsKey("address")) form.setAddress(values.get("address").toString());
        if (values.containsKey("businessType"))
            form.setBusinessType(values.get("businessType").toString());
        if (values.containsKey("businessItem"))
            form.setBusinessItem(values.get("businessItem").toString());
        if (values.containsKey("planText")) form.setPlanText(values.get("planText").toString());
        try {
            if (values.containsKey("openedOn"))
                form.setOpenedOn(LocalDate.parse(values.get("openedOn").toString()));
        } catch (RuntimeException ignored) {
            /* 읽지 못한 날짜는 기존값·직접 입력으로 확인한다. */
        }
        try {
            if (values.containsKey("requestedAmount"))
                form.setRequestedAmount(new BigDecimal(values.get("requestedAmount").toString()));
        } catch (RuntimeException ignored) {
            /* 금액 후보의 오류를 신청값으로 확정하지 않는다. */
        }
        try {
            if (values.containsKey("selfContribution"))
                form.setSelfContribution(new BigDecimal(values.get("selfContribution").toString()));
        } catch (RuntimeException ignored) {
            /* 금액 후보의 오류를 신청값으로 확정하지 않는다. */
        }
        return form;
    }

    @SuppressWarnings("unchecked")
    public Path asset(PortalPrincipal actor, long id, String key) {
        var job = jobs.find(id);
        if (job == null) throw new BusinessException(404, "문서 분석 결과가 없습니다.");
        workflow.view(actor, ((Number) job.get("application_id")).longValue());
        var permitted = new ArrayList<String>();
        jobs.sources(id).forEach(source -> permitted.add(source.get("storage_key").toString()));
        var result = result(job);
        for (var page : (List<Map<String, Object>>) result.getOrDefault("pages", List.of())) {
            permitted.add(page.get("previewKey").toString());
            for (var photo : (List<Map<String, Object>>) page.get("photos"))
                permitted.add(photo.get("storageKey").toString());
        }
        for (var group : (List<Map<String, Object>>) result.getOrDefault("groups", List.of()))
            permitted.add(group.get("pdfKey").toString());
        if (!permitted.contains(key)) throw BusinessException.forbidden();
        return storage.resolve(key);
    }

    public String authorizedFilename(PortalPrincipal actor, long intakeId, String key) {
        asset(actor, intakeId, key);
        return jobs.sources(intakeId).stream()
                .filter(source -> key.equals(source.get("storage_key")))
                .map(source -> source.get("original_name").toString())
                .findFirst()
                .orElse(key.endsWith(".pdf") ? "분할서류.pdf" : "문서이미지.png");
    }

    @SuppressWarnings("unchecked")
    public boolean apply(
            PortalPrincipal actor,
            long applicationId,
            long intakeId,
            ApplicationForm form,
            List<String> kinds,
            List<Integer> starts,
            List<String> selectedPhotos) {
        var job = view(actor, applicationId, intakeId);
        if (job == null) throw new BusinessException(404, "분석 결과를 찾을 수 없습니다.");
        var application = workflow.view(actor, applicationId);
        access.owner(actor, application);
        if ("APPLIED".equals(job.get("status"))) return hasRequiredDocuments(applicationId);
        editable(actor, application);
        if (!"READY".equals(job.get("status")))
            throw new BusinessException(409, "문서 분석 완료 후 반영해 주세요.");
        var result = result(job);
        var pages = (List<Map<String, Object>>) result.get("pages");
        var oldGroups = (List<Map<String, Object>>) result.get("groups");
        var groups = grouping.group(pages, kinds, starts);
        var generated = new ArrayList<String>();
        boolean[] committed = {false};
        try {
            // PDF 분할 호출을 완료한 뒤 짧은 DB 트랜잭션에서 버전·상태를 재확인한다.
            for (var group : groups) {
                var same =
                        oldGroups.stream()
                                .filter(
                                        old ->
                                                old.get("sourceId")
                                                                .toString()
                                                                .equals(
                                                                        group.get("sourceId")
                                                                                .toString())
                                                        && old.get("pageNumbers")
                                                                .equals(group.get("pageNumbers")))
                                .findFirst();
                if (same.isPresent()) group.put("pdfKey", same.get().get("pdfKey"));
                else {
                    var source =
                            jobs.sources(intakeId).stream()
                                    .filter(
                                            item ->
                                                    item.get("id")
                                                            .toString()
                                                            .equals(
                                                                    group.get("sourceId")
                                                                            .toString()))
                                    .findFirst()
                                    .orElseThrow();
                    String key =
                            storage.save(
                                    provider.split(
                                            storage.resolve(source.get("storage_key").toString()),
                                            (List<Integer>) group.get("pageNumbers")),
                                    true);
                    generated.add(key);
                    group.put("pdfKey", key);
                }
            }
            result.put("groups", groups);
            for (var page : pages)
                for (var photo : (List<Map<String, Object>>) page.get("photos"))
                    photo.put(
                            "selected",
                            selectedPhotos.contains(photo.get("storageKey").toString()));
            boolean complete =
                    Boolean.TRUE.equals(
                            transaction.execute(
                                    status -> {
                                        var locked = applications.lock(applicationId);
                                        editable(actor, locked);
                                        var current = jobs.lock(intakeId);
                                        if (!"READY".equals(current.get("status")))
                                            throw new BusinessException(409, "이미 반영한 문서입니다.");
                                        workflow.save(actor, applicationId, form);
                                        for (String type : List.of("BUSINESS_CERT", "ESTIMATE")) {
                                            var page =
                                                    pages.stream()
                                                            .filter(
                                                                    item ->
                                                                            type.equals(
                                                                                    item.get(
                                                                                            "kind")))
                                                            .findFirst();
                                            if (page.isPresent()) {
                                                try {
                                                    documents.upload(
                                                            actor,
                                                            applicationId,
                                                            type,
                                                            IntakePolicy.label(type) + "-문서추출.png",
                                                            Files.readAllBytes(
                                                                    storage.resolve(
                                                                            page.get()
                                                                                    .get(
                                                                                            "previewKey")
                                                                                    .toString())));
                                                } catch (IOException exception) {
                                                    throw new IllegalStateException(
                                                            "분할 서류를 연결하지 못했습니다.", exception);
                                                }
                                            }
                                        }
                                        var certificate =
                                                documentMapper.current(applicationId).stream()
                                                        .filter(
                                                                document ->
                                                                        "BUSINESS_CERT"
                                                                                .equals(
                                                                                        document
                                                                                                .getTypeCode()))
                                                        .findFirst();
                                        if (certificate.isPresent()
                                                && form.getBusinessName() != null
                                                && form.getBusinessNo() != null
                                                && form.getRepresentative() != null
                                                && form.getOpenedOn() != null)
                                            ocr.confirm(
                                                    actor,
                                                    certificate.get().getId(),
                                                    Map.of(
                                                            "businessName",
                                                            form.getBusinessName(),
                                                            "businessNo",
                                                            form.getBusinessNo(),
                                                            "representative",
                                                            form.getRepresentative(),
                                                            "openedOn",
                                                            form.getOpenedOn().toString()));
                                        if (jobs.applied(intakeId, json.write(result)) != 1)
                                            throw BusinessException.conflict();
                                        audit.record(
                                                actor,
                                                applicationId,
                                                "DOCUMENT_INTAKE_APPLIED",
                                                "INTAKE",
                                                intakeId,
                                                "검토한 문서·분류·사진 반영");
                                        return hasRequiredDocuments(applicationId);
                                    }));
            committed[0] = true;
            return complete;
        } finally {
            if (!committed[0]) generated.forEach(storage::delete);
        }
    }

    @Transactional
    public void saveReviewedContent(
            PortalPrincipal actor, long applicationId, long intakeId, ApplicationForm form) {
        var application = applications.lock(applicationId);
        editable(actor, application);
        var job = jobs.lock(intakeId);
        if (job == null || ((Number) job.get("application_id")).longValue() != applicationId) {
            throw BusinessException.forbidden();
        }
        if (!"APPLIED".equals(job.get("status"))) {
            throw new BusinessException(409, "문서 내용을 먼저 반영한 뒤 수정해 주세요.");
        }
        // 추출 원문은 보존하고 사용자가 수정한 신청값만 버전 검사와 함께 저장한다.
        workflow.save(actor, applicationId, form);
        var certificate =
                documentMapper.current(applicationId).stream()
                        .filter(document -> "BUSINESS_CERT".equals(document.getTypeCode()))
                        .findFirst();
        if (certificate.isPresent()
                && form.getBusinessName() != null
                && form.getBusinessNo() != null
                && form.getRepresentative() != null
                && form.getOpenedOn() != null) {
            ocr.confirm(
                    actor,
                    certificate.get().getId(),
                    Map.of(
                            "businessName",
                            form.getBusinessName(),
                            "businessNo",
                            form.getBusinessNo(),
                            "representative",
                            form.getRepresentative(),
                            "openedOn",
                            form.getOpenedOn().toString()));
        }
        audit.record(
                actor,
                applicationId,
                "INTAKE_CONTENT_REVISED",
                "INTAKE",
                intakeId,
                "검토 화면에서 신청 내용 수정");
    }

    private boolean hasRequiredDocuments(long applicationId) {
        return documentMapper.current(applicationId).stream()
                .map(document -> document.getTypeCode())
                .toList()
                .containsAll(List.of("BUSINESS_CERT", "ESTIMATE"));
    }
}
