package kr.co.maeum.support.service;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.mapper.ApplicationMapper;
import kr.co.maeum.support.mapper.IdentityMapper;
import kr.co.maeum.support.mapper.ReviewMapper;
import kr.co.maeum.support.model.ApplicationStatus;
import kr.co.maeum.support.model.SupplementRequest;
import kr.co.maeum.support.security.AccessScope;
import kr.co.maeum.support.security.PortalPrincipal;
import kr.co.maeum.support.service.policy.ApplicationPolicy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Service
public class ReviewService {
    private final ApplicationMapper applications;
    private final ApplicationService workflow;
    private final ApplicationPolicy policy;
    private final AccessScope access;
    private final ReviewMapper reviews;
    private final IdentityMapper identities;
    private final AuditService audit;
    private final Clock clock;

    public ReviewService(
            ApplicationMapper applications,
            ApplicationService workflow,
            ApplicationPolicy policy,
            AccessScope access,
            ReviewMapper reviews,
            IdentityMapper identities,
            AuditService audit,
            Clock clock) {
        this.applications = applications;
        this.workflow = workflow;
        this.policy = policy;
        this.access = access;
        this.reviews = reviews;
        this.identities = identities;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public void assign(PortalPrincipal actor, long applicationId, long staffId, String reason) {
        access.role(actor, "ADMIN");
        var application = applications.lock(applicationId);
        access.view(actor, application);
        policy.text(reason, "배정 사유", 1000, true);
        var staff =
                identities.listStaff(actor.workspaceId()).stream()
                        .filter(account -> account.id() == staffId)
                        .findFirst()
                        .orElse(null);
        if (staff == null) {
            throw new BusinessException(422, "배정 가능한 담당자가 없습니다.");
        }
        applications.assign(
                Map.of(
                        "applicationId",
                        applicationId,
                        "staffId",
                        staff.id(),
                        "assignedBy",
                        actor.id(),
                        "reason",
                        reason));
        audit.record(actor, applicationId, "STAFF_ASSIGNED", "APPLICATION", applicationId, reason);
    }

    @Transactional
    public void start(PortalPrincipal actor, long id, long version) {
        var application = applications.lock(id);
        access.reviewer(actor, application);
        policy.expectVersion(application, version);
        policy.expectStatus(application, ApplicationStatus.SUBMITTED);
        workflow.changeStatus(actor, application, ApplicationStatus.UNDER_REVIEW, "검토 시작");
        saveReview(id, actor.id(), null, null);
    }

    @Transactional
    public void supplement(
            PortalPrincipal actor,
            long id,
            long version,
            String target,
            String reason,
            LocalDateTime dueAt) {
        var application = applications.lock(id);
        access.reviewer(actor, application);
        policy.expectVersion(application, version);
        policy.expectStatus(application, ApplicationStatus.UNDER_REVIEW);
        if (!List.of("BUSINESS_CERT", "ESTIMATE", "businessName", "planText").contains(target)) {
            throw new BusinessException(422, "보완 대상을 확인해 주세요.");
        }
        policy.text(reason, "보완 사유", 1000, true);
        if (dueAt == null || !dueAt.isAfter(LocalDateTime.now(clock))) {
            throw new BusinessException(422, "보완 기한은 현재 이후로 지정해 주세요.");
        }
        var request = new SupplementRequest();
        request.setApplicationId(id);
        request.setRequestedBy(actor.id());
        request.setReason(reason);
        request.setDueAt(dueAt);
        reviews.insertSupplement(request);
        reviews.insertItem(
                Map.of("requestId", request.getId(), "target", target, "instruction", reason));
        workflow.changeStatus(actor, application, ApplicationStatus.NEEDS_SUPPLEMENT, reason);
    }

    @Transactional
    public void decide(
            PortalPrincipal actor,
            long id,
            long version,
            String decision,
            String reason,
            boolean checked) {
        var application = applications.lock(id);
        access.reviewer(actor, application);
        policy.expectVersion(application, version);
        policy.expectStatus(application, ApplicationStatus.UNDER_REVIEW);
        if (!List.of("APPROVED", "REJECTED").contains(decision) || !checked) {
            throw new BusinessException(422, "검토 확인 및 결과를 선택해 주세요.");
        }
        policy.text(reason, "결과 사유", 1000, true);
        saveReview(id, actor.id(), decision, reason);
        workflow.changeStatus(actor, application, ApplicationStatus.valueOf(decision), reason);
    }

    private void saveReview(long id, long reviewerId, String decision, String reason) {
        var review = new HashMap<String, Object>();
        review.put("applicationId", id);
        review.put("reviewerId", reviewerId);
        review.put(
                "checkItems",
                decision == null ? "{\"documentsChecked\":false}" : "{\"documentsChecked\":true}");
        review.put("decision", decision);
        review.put("reason", reason);
        reviews.saveReview(review);
    }
}
