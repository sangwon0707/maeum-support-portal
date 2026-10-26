package kr.co.maeum.support.service;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.common.JsonCodec;
import kr.co.maeum.support.dto.ApplicationForm;
import kr.co.maeum.support.mapper.ApplicationMapper;
import kr.co.maeum.support.mapper.DocumentMapper;
import kr.co.maeum.support.mapper.OcrMapper;
import kr.co.maeum.support.mapper.ProgramMapper;
import kr.co.maeum.support.mapper.ReviewMapper;
import kr.co.maeum.support.model.Application;
import kr.co.maeum.support.model.ApplicationStatus;
import kr.co.maeum.support.security.AccessScope;
import kr.co.maeum.support.security.PortalPrincipal;
import kr.co.maeum.support.service.policy.ApplicationPolicy;

import org.egovframe.rte.fdl.cmmn.EgovAbstractServiceImpl;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.HashMap;
import java.util.HexFormat;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.TreeMap;

@Service
public class ApplicationService extends EgovAbstractServiceImpl {
    private final ApplicationMapper applications;
    private final ProgramMapper programs;
    private final DocumentMapper documents;
    private final ReviewMapper reviews;
    private final OcrMapper ocr;
    private final AccessScope access;
    private final ApplicationPolicy policy;
    private final AuditService audit;
    private final JsonCodec json;
    private final Clock clock;

    public ApplicationService(
            ApplicationMapper applications,
            ProgramMapper programs,
            DocumentMapper documents,
            ReviewMapper reviews,
            OcrMapper ocr,
            AccessScope access,
            ApplicationPolicy policy,
            AuditService audit,
            JsonCodec json,
            Clock clock) {
        this.applications = applications;
        this.programs = programs;
        this.documents = documents;
        this.reviews = reviews;
        this.ocr = ocr;
        this.access = access;
        this.policy = policy;
        this.audit = audit;
        this.json = json;
        this.clock = clock;
    }

    @Transactional
    public long start(PortalPrincipal actor, long programId) {
        access.role(actor, "APPLICANT");
        var program = programs.find(programId);
        if (program == null || !"PUBLISHED".equals(program.getStatus())) {
            throw new BusinessException(404, "신청 가능한 공고가 아닙니다.");
        }
        // 사용자 잠금 뒤 최신 회차를 잠금 조회해 REPEATABLE READ의 이전 스냅샷을 피한다.
        applications.lockOwner(actor.id());
        var existing = applications.findOwned(actor.workspaceId(), actor.id(), programId);
        boolean repeatPractice = "applicant".equals(actor.username()) && !actor.demo();
        if (existing != null
                && (!repeatPractice
                        || "DRAFT".equals(existing.getStatus())
                        || "NEEDS_SUPPLEMENT".equals(existing.getStatus()))) {
            return existing.getId();
        }
        var application = new Application();
        application.setWorkspaceId(actor.workspaceId());
        application.setProgramId(programId);
        application.setOwnerId(actor.id());
        if (repeatPractice) {
            applications.insertPracticeRound(application);
        } else {
            applications.insert(application);
        }
        audit.record(
                actor,
                application.getId(),
                "DRAFT_CREATED",
                "APPLICATION",
                application.getId(),
                "신청서 생성");
        return application.getId();
    }

    public Application view(PortalPrincipal actor, long id) {
        var application = applications.find(id);
        access.view(actor, application);
        return application;
    }

    @Transactional
    public void save(PortalPrincipal actor, long id, ApplicationForm form) {
        var application = applications.lock(id);
        access.owner(actor, application);
        policy.expectVersion(application, form.getVersion() == null ? -1 : form.getVersion());
        boolean supplement = "NEEDS_SUPPLEMENT".equals(application.getStatus());
        List<String> allowed =
                List.of(
                        "businessName",
                        "businessNo",
                        "representative",
                        "openedOn",
                        "phone",
                        "planText",
                        "requestedAmount",
                        "address",
                        "businessType",
                        "businessItem",
                        "selfContribution");
        if (supplement) {
            var request = reviews.openSupplement(id);
            if (request == null || !LocalDateTime.now(clock).isBefore(request.getDueAt())) {
                throw new BusinessException(422, "보완 기한이 지났습니다.");
            }
            allowed = reviews.targets(request.getId());
        } else {
            policy.expectStatus(application, ApplicationStatus.DRAFT);
        }
        apply(application, form, allowed);
        policy.validate(
                application,
                programs.find(application.getProgramId()).getMaxAmount(),
                false,
                LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))));
        persist(application);
    }

    private void apply(Application target, ApplicationForm form, List<String> allowed) {
        // 보완 시 readonly 속성은 신뢰하지 않는다. 서버에서도 허용 필드 외 변경을 거절한다.
        checkAllowed("businessName", target.getBusinessName(), form.getBusinessName(), allowed);
        target.setBusinessName(form.getBusinessName());
        checkAllowed("businessNo", target.getBusinessNo(), form.getBusinessNo(), allowed);
        target.setBusinessNo(form.getBusinessNo());
        checkAllowed(
                "representative", target.getRepresentative(), form.getRepresentative(), allowed);
        target.setRepresentative(form.getRepresentative());
        checkAllowed("openedOn", target.getOpenedOn(), form.getOpenedOn(), allowed);
        target.setOpenedOn(form.getOpenedOn());
        checkAllowed("phone", target.getPhone(), form.getPhone(), allowed);
        target.setPhone(form.getPhone());
        checkAllowed("planText", target.getPlanText(), form.getPlanText(), allowed);
        target.setPlanText(form.getPlanText());
        boolean amountEqual =
                target.getRequestedAmount() != null
                        && form.getRequestedAmount() != null
                        && target.getRequestedAmount().compareTo(form.getRequestedAmount()) == 0;
        if (!amountEqual) {
            checkAllowed(
                    "requestedAmount",
                    target.getRequestedAmount(),
                    form.getRequestedAmount(),
                    allowed);
        }
        target.setRequestedAmount(form.getRequestedAmount());
        checkAllowed("address", target.getAddress(), form.getAddress(), allowed);
        target.setAddress(form.getAddress());
        checkAllowed("businessType", target.getBusinessType(), form.getBusinessType(), allowed);
        target.setBusinessType(form.getBusinessType());
        checkAllowed("businessItem", target.getBusinessItem(), form.getBusinessItem(), allowed);
        target.setBusinessItem(form.getBusinessItem());
        checkAllowed(
                "selfContribution",
                target.getSelfContribution(),
                form.getSelfContribution(),
                allowed);
        target.setSelfContribution(form.getSelfContribution());
    }

    private void checkAllowed(
            String field, Object original, Object submitted, List<String> allowed) {
        if (!allowed.contains(field) && !Objects.equals(original, submitted)) {
            throw new BusinessException(422, "보완 요청을 받은 항목만 수정할 수 있습니다.");
        }
    }

    @Transactional
    public long submit(
            PortalPrincipal actor, long id, long expectedVersion, String key, String responseText) {
        var application = applications.lock(id);
        access.owner(actor, application);
        if (key == null || !key.matches("[a-zA-Z0-9-]{16,64}")) {
            throw new BusinessException(422, "제출 요청 식별자가 유효하지 않습니다.");
        }
        var attached = documents.current(id);
        var snapshot = new TreeMap<String, Object>();
        snapshot.put("schemaVersion", 3);
        snapshot.put("sourceIntakeId", applications.appliedIntakeId(id));
        snapshot.put("address", application.getAddress());
        snapshot.put("businessType", application.getBusinessType());
        snapshot.put("businessItem", application.getBusinessItem());
        snapshot.put("selfContribution", application.getSelfContribution());
        snapshot.put("totalProjectCost", application.getTotalProjectCost());
        snapshot.put("businessName", application.getBusinessName());
        snapshot.put("businessNo", application.getBusinessNo());
        snapshot.put("representative", application.getRepresentative());
        snapshot.put("openedOn", application.getOpenedOn());
        snapshot.put("phone", application.getPhone());
        snapshot.put("planText", application.getPlanText());
        snapshot.put("requestedAmount", application.getRequestedAmount());
        snapshot.put("documents", attached.stream().map(document -> document.getId()).toList());
        snapshot.put(
                "confirmations",
                attached.stream()
                        .flatMap(document -> ocr.confirmations(document.getId()).stream())
                        .toList());
        snapshot.put("responseText", responseText == null ? "" : responseText);
        String payload = json.write(snapshot);
        String hash = hash(payload);
        Long prior = applications.submittedCommand(id, key, hash);
        if (prior != null) {
            return prior;
        }
        if (applications.commandExists(id, key) > 0) {
            throw BusinessException.conflict();
        }
        policy.expectVersion(application, expectedVersion);
        var program = programs.find(application.getProgramId());
        boolean supplement = "NEEDS_SUPPLEMENT".equals(application.getStatus());
        var request = supplement ? reviews.openSupplement(id) : null;
        if (supplement) {
            if (request == null) {
                throw BusinessException.conflict();
            }
            policy.deadline(LocalDateTime.now(clock), LocalDateTime.MIN, request.getDueAt());
            policy.text(responseText, "보완 답변", 2000, true);
        } else {
            policy.expectStatus(application, ApplicationStatus.DRAFT);
            if (!"PUBLISHED".equals(program.getStatus())) {
                throw new BusinessException(422, "접수가 종료된 공고입니다.");
            }
            policy.deadline(LocalDateTime.now(clock), program.getOpensAt(), program.getClosesAt());
        }
        policy.validate(
                application,
                program.getMaxAmount(),
                true,
                LocalDate.now(clock.withZone(ZoneId.of("Asia/Seoul"))));
        var attachedTypes = attached.stream().map(document -> document.getTypeCode()).toList();
        if (!attachedTypes.containsAll(programs.requiredTypes(program.getId()))) {
            throw new BusinessException(422, "필수 서류가 누락되었습니다. 사업자등록증과 견적서를 확인해 주세요.");
        }
        if (supplement) {
            // 서류 보완은 새 버전이 제출되었는지 직전 제출본과 비교한다.
            for (String target : reviews.targets(request.getId())) {
                if (("BUSINESS_CERT".equals(target) || "ESTIMATE".equals(target))
                        && !documentChanged(id, target)) {
                    throw new BusinessException(422, "보완 요청한 서류를 새 버전으로 교체해 주세요.");
                }
            }
        }
        var revision = new HashMap<String, Object>();
        revision.put("applicationId", id);
        revision.put("snapshot", payload);
        revision.put("submittedBy", actor.id());
        applications.revision(revision);
        long revisionId = ((Number) revision.get("id")).longValue();
        attached.forEach(document -> applications.revisionDocument(revisionId, document.getId()));
        if (supplement) {
            reviews.answer(
                    Map.of(
                            "requestId",
                            request.getId(),
                            "revisionId",
                            revisionId,
                            "text",
                            responseText,
                            "actorId",
                            actor.id()));
            reviews.closeSupplement(request.getId());
        }
        if (application.getReceiptNo() == null) {
            application.setReceiptNo("SUP-" + String.format("%08d", id));
        }
        if (application.getSubmittedAt() == null) {
            application.setSubmittedAt(LocalDateTime.now(clock));
        }
        changeStatus(
                actor, application, ApplicationStatus.SUBMITTED, supplement ? "보완 재제출" : "신청 제출");
        applications.command(
                Map.of("applicationId", id, "key", key, "hash", hash, "revisionId", revisionId));
        return revisionId;
    }

    private boolean documentChanged(long applicationId, String type) {
        return applications.changedDocument(applicationId, type) == 1;
    }

    public void persist(Application application) {
        if (applications.update(application) != 1) {
            throw BusinessException.conflict();
        }
        application.setVersion(application.getVersion() + 1);
    }

    public void changeStatus(
            PortalPrincipal actor,
            Application application,
            ApplicationStatus target,
            String reason) {
        String from = application.getStatus();
        application.setStatus(target.name());
        persist(application);
        applications.history(
                Map.of(
                        "applicationId",
                        application.getId(),
                        "from",
                        from,
                        "to",
                        target.name(),
                        "actorId",
                        actor.id(),
                        "reason",
                        reason));
        audit.record(
                actor,
                application.getId(),
                "STATUS_CHANGED",
                "APPLICATION",
                application.getId(),
                from + " → " + target.name());
    }

    private String hash(String value) {
        try {
            return HexFormat.of()
                    .formatHex(
                            MessageDigest.getInstance("SHA-256")
                                    .digest(value.getBytes(StandardCharsets.UTF_8)));
        } catch (NoSuchAlgorithmException exception) {
            throw new IllegalStateException(exception);
        }
    }
}
