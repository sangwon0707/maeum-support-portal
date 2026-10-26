package kr.co.maeum.support.service;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.config.OcrSettings;
import kr.co.maeum.support.dto.ApplicationForm;
import kr.co.maeum.support.mapper.ApplicationMapper;
import kr.co.maeum.support.mapper.DocumentMapper;
import kr.co.maeum.support.mapper.OcrMapper;
import kr.co.maeum.support.security.AccessScope;
import kr.co.maeum.support.security.PortalPrincipal;

import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class OcrService {
    private final OcrMapper jobs;
    private final DocumentMapper documents;
    private final ApplicationMapper applications;
    private final AccessScope access;
    private final AuditService audit;
    private final OcrSettings settings;
    private final ApplicationService workflow;

    public OcrService(
            OcrMapper jobs,
            DocumentMapper documents,
            ApplicationMapper applications,
            AccessScope access,
            AuditService audit,
            OcrSettings settings,
            ApplicationService workflow) {
        this.jobs = jobs;
        this.documents = documents;
        this.applications = applications;
        this.access = access;
        this.audit = audit;
        this.settings = settings;
        this.workflow = workflow;
    }

    @Transactional
    public void request(PortalPrincipal actor, long documentId) {
        request(actor, documentId, "SAMPLE");
    }

    @Transactional
    public void request(PortalPrincipal actor, long documentId, String mode) {
        var document = documents.find(documentId);
        if (document == null) {
            throw new BusinessException(404, "서류를 찾을 수 없습니다.");
        }
        var application = applications.lock(document.getApplicationId());
        access.owner(actor, application);
        ensureCurrent(documentId, application.getId());
        if (!"BUSINESS_CERT".equals(document.getTypeCode())
                || !("DRAFT".equals(application.getStatus())
                        || "NEEDS_SUPPLEMENT".equals(application.getStatus()))) {
            throw new BusinessException(409, "작성 중인 사업자등록증만 OCR 요청할 수 있습니다.");
        }
        if (("SAMPLE".equals(mode) && !actor.demo())
                || !List.of("SAMPLE", "LIVE", "LIVE_AI").contains(mode)) {
            throw new BusinessException(422, "OCR 처리 방식을 확인해 주세요.");
        }
        if ((!mode.equals("SAMPLE") && !settings.liveEnabled())
                || (mode.equals("LIVE_AI") && !settings.aiEnabled())) {
            throw new BusinessException(409, "이 서버에서 선택한 인식 기능을 사용할 수 없습니다.");
        }
        if (jobs.findModeJob(documentId, mode) != null) {
            return;
        }
        if ((actor.demo() && jobs.countWorkspace(actor.workspaceId()) >= 10)
                || (!actor.demo() && jobs.countOwnerToday(actor.workspaceId(), actor.id()) >= 20)) {
            throw new BusinessException(429, "문자 인식 요청 한도에 도달했습니다. 원본을 보고 직접 확인값을 입력해 주세요.");
        }
        if (mode.equals("LIVE_AI")) {
            // 반드시 전역 → 공간 순서로 예약해 서로 다른 신청의 동시 요청도 같은 한도를 공유한다.
            reserveAi("GLOBAL", settings.globalAiLimit());
            reserveAi(
                    actor.demo() ? "WORKSPACE:" + actor.workspaceId() : "USER:" + actor.id(),
                    settings.actorAiLimit());
        }
        jobs.insert(documentId, mode, mode.equals("SAMPLE") ? "sample-v1" : "paddle-v5-fields-v1");
        audit.record(
                actor,
                application.getId(),
                "OCR_REQUESTED",
                "DOCUMENT",
                documentId,
                "OCR 요청 " + mode);
    }

    @Transactional
    public void confirm(PortalPrincipal actor, long documentId, Map<String, String> fields) {
        var document = documents.find(documentId);
        if (document == null) {
            throw new BusinessException(404, "서류를 찾을 수 없습니다.");
        }
        var application = applications.lock(document.getApplicationId());
        access.owner(actor, application);
        ensureCurrent(documentId, application.getId());
        if (!("DRAFT".equals(application.getStatus())
                || "NEEDS_SUPPLEMENT".equals(application.getStatus()))) {
            throw new BusinessException(409, "제출 후에는 확인값을 변경할 수 없습니다.");
        }
        var result = jobs.resultFor(documentId);
        for (String field : List.of("businessName", "businessNo", "representative", "openedOn")) {
            String value = fields.get(field);
            if (value == null || value.isBlank() || value.length() > 200) {
                throw new BusinessException(422, "확인할 네 항목을 모두 입력해 주세요.");
            }
            if (field.equals("businessNo") && !value.matches("[0-9]{10}")) {
                throw new BusinessException(422, "확인한 사업자번호는 숫자 10자리여야 합니다.");
            }
            if (field.equals("openedOn")) {
                try {
                    LocalDate.parse(value);
                } catch (DateTimeParseException exception) {
                    throw new BusinessException(422, "개업일 형식을 확인해 주세요.");
                }
            }
            var confirmation = new HashMap<String, Object>();
            confirmation.put("documentId", documentId);
            confirmation.put("resultId", result == null ? null : result.get("id"));
            confirmation.put("field", field);
            confirmation.put("value", value);
            confirmation.put("actorId", actor.id());
            jobs.confirm(confirmation);
        }
        audit.record(
                actor,
                application.getId(),
                "OCR_CONFIRMED",
                "DOCUMENT",
                documentId,
                "추출값과 별도로 사용자 확인값 저장");
    }

    @Transactional
    public void applyConfirmed(PortalPrincipal actor, long documentId, long expectedVersion) {
        var document = documents.find(documentId);
        if (document == null) {
            throw new BusinessException(404, "서류가 없습니다.");
        }
        var application = applications.lock(document.getApplicationId());
        access.owner(actor, application);
        ensureCurrent(documentId, application.getId());
        var values = new HashMap<String, String>();
        jobs.confirmations(documentId)
                .forEach(
                        row ->
                                values.put(
                                        row.get("field_code").toString(),
                                        row.get("confirmed_value").toString()));
        if (values.size() != 4) {
            throw new BusinessException(422, "원본을 보고 네 항목의 확인값을 먼저 저장해 주세요.");
        }
        var form = new ApplicationForm();
        BeanUtils.copyProperties(application, form);
        form.setVersion(expectedVersion);
        form.setBusinessName(values.get("businessName"));
        form.setBusinessNo(values.get("businessNo"));
        form.setRepresentative(values.get("representative"));
        form.setOpenedOn(LocalDate.parse(values.get("openedOn")));
        workflow.save(actor, application.getId(), form);
        audit.record(
                actor,
                application.getId(),
                "CONFIRMED_FIELDS_APPLIED",
                "DOCUMENT",
                documentId,
                "사용자가 확인한 네 항목을 신청서에 반영");
    }

    private void reserveAi(String scope, int limit) {
        jobs.ensureAiQuota(scope);
        if (jobs.reserveAiQuota(scope, limit) != 1) {
            throw new BusinessException(429, "오늘의 AI 보조 요청 한도에 도달했습니다. 로컬 OCR을 이용해 주세요.");
        }
    }

    private void ensureCurrent(long documentId, long applicationId) {
        if (documents.current(applicationId).stream()
                .noneMatch(document -> document.getId() == documentId)) {
            throw new BusinessException(409, "교체되기 전 서류입니다. 현재 서류를 다시 열어 주세요.");
        }
    }
}
