package kr.co.maeum.support.demo;

import kr.co.maeum.support.application.ApplicationForm;
import kr.co.maeum.support.application.ApplicationMapper;
import kr.co.maeum.support.application.ApplicationService;
import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.document.DocumentMapper;
import kr.co.maeum.support.document.DocumentService;
import kr.co.maeum.support.identity.IdentityMapper;
import kr.co.maeum.support.identity.PortalPrincipal;
import kr.co.maeum.support.ocr.OcrService;
import kr.co.maeum.support.program.ProgramMapper;
import kr.co.maeum.support.review.ReviewService;

import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
public class DemoService {
    private final DemoMapper demos;
    private final IdentityMapper identities;
    private final BCryptPasswordEncoder encoder;
    private final ProgramMapper programs;
    private final ApplicationService workflow;
    private final ApplicationMapper applications;
    private final DocumentService documents;
    private final DocumentMapper documentMapper;
    private final OcrService ocr;
    private final ReviewService reviews;
    private final Clock clock;

    public DemoService(
            DemoMapper demos,
            IdentityMapper identities,
            BCryptPasswordEncoder encoder,
            ProgramMapper programs,
            ApplicationService workflow,
            ApplicationMapper applications,
            DocumentService documents,
            DocumentMapper documentMapper,
            OcrService ocr,
            ReviewService reviews,
            Clock clock) {
        this.demos = demos;
        this.identities = identities;
        this.encoder = encoder;
        this.programs = programs;
        this.workflow = workflow;
        this.applications = applications;
        this.documents = documents;
        this.documentMapper = documentMapper;
        this.ocr = ocr;
        this.reviews = reviews;
        this.clock = clock;
    }

    @Transactional
    public PortalPrincipal start(String scenario, String sessionTokenHash) {
        Long scenarioId = demos.scenario(scenario);
        if (scenarioId == null) {
            throw new BusinessException(422, "예시 시나리오를 선택해 주세요.");
        }
        var space = new HashMap<String, Object>();
        space.put("scenarioId", scenarioId);
        space.put("tokenHash", sessionTokenHash);
        demos.insertWorkspace(space);
        long workspaceId = ((Number) space.get("id")).longValue();
        String suffix = UUID.randomUUID().toString();
        createUser(
                workspaceId,
                "demo-applicant-" + suffix,
                "가상 신청자",
                "APPLICANT",
                UUID.randomUUID().toString());
        createUser(
                workspaceId,
                "demo-staff-" + suffix,
                "가상 담당자",
                "STAFF",
                UUID.randomUUID().toString());
        var applicant = identities.findRole(workspaceId, "APPLICANT").principal();
        var staff = identities.findRole(workspaceId, "STAFF").principal();
        var program =
                programs.list("", "OPEN", false).stream()
                        .findFirst()
                        .orElseThrow(() -> new BusinessException(422, "접수 중인 예시 사업이 없습니다."));
        long id = workflow.start(applicant, program.getId());
        var form = new ApplicationForm();
        form.setVersion(applications.find(id).getVersion());
        form.setAddress("가상시 예시구 도움로 123, 1층");
        form.setBusinessType("도매 및 소매업");
        form.setBusinessItem("생활잡화 소매");
        form.setSelfContribution(new BigDecimal("300000"));
        form.setBusinessName("가상상점");
        form.setBusinessNo("0000000000");
        form.setRepresentative("가상대표");
        form.setOpenedOn(LocalDate.of(2024, 1, 1));
        form.setPhone("000-0000-0000");
        form.setPlanText("매장 간판과 조명을 개선하여 방문 고객의 이용 편의를 높이고자 합니다. (가상 신청)");
        form.setRequestedAmount(new BigDecimal("3000000"));
        workflow.save(applicant, id, form);
        documents.attachFixture(applicant, id, "BUSINESS_CERT");
        if (!"MISSING".equals(scenario)) {
            documents.attachFixture(applicant, id, "ESTIMATE");
        }
        // 체험 데이터도 같은 제출·검토 서비스를 지나므로 실제 업무 규칙과 어긋나지 않는다.
        applications.assign(
                Map.of(
                        "applicationId",
                        id,
                        "staffId",
                        staff.id(),
                        "assignedBy",
                        staff.id(),
                        "reason",
                        "가상 체험 자동 배정"));
        long certificate =
                documentMapper.current(id).stream()
                        .filter(document -> document.getTypeCode().equals("BUSINESS_CERT"))
                        .findFirst()
                        .orElseThrow()
                        .getId();
        ocr.request(applicant, certificate);
        if ("SUPPLEMENT".equals(scenario)) {
            workflow.submit(
                    applicant,
                    id,
                    applications.find(id).getVersion(),
                    UUID.randomUUID().toString(),
                    null);
            reviews.start(staff, id, applications.find(id).getVersion());
            reviews.supplement(
                    staff,
                    id,
                    applications.find(id).getVersion(),
                    "ESTIMATE",
                    "세부 항목이 보이는 견적서로 보완해 주세요.",
                    LocalDateTime.now(clock).plusDays(3));
        }
        return applicant;
    }

    public void createUser(
            long workspaceId, String loginId, String displayName, String role, String password) {
        var user = new HashMap<String, Object>();
        user.put("workspaceId", workspaceId);
        user.put("loginId", loginId);
        user.put("displayName", displayName);
        user.put("passwordHash", encoder.encode(password));
        identities.insertAccount(user);
        identities.insertRole(((Number) user.get("id")).longValue(), role);
    }

    public PortalPrincipal switchRole(PortalPrincipal actor, String role) {
        if (actor == null
                || !actor.demo()
                || !List.of("APPLICANT", "STAFF").contains(role)
                || identities.activeWorkspace(actor.workspaceId()) != 1) {
            throw BusinessException.forbidden();
        }
        return identities.findRole(actor.workspaceId(), role).principal();
    }

    public void expire(PortalPrincipal actor) {
        if (actor != null && actor.demo()) {
            demos.expire(actor.workspaceId());
        }
    }
}
