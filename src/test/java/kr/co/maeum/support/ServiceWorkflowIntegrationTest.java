package kr.co.maeum.support;

import static org.junit.jupiter.api.Assertions.*;

import kr.co.maeum.support.application.ApplicationForm;
import kr.co.maeum.support.application.ApplicationMapper;
import kr.co.maeum.support.application.ApplicationService;
import kr.co.maeum.support.application.SampleApplicationService;
import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.config.WebConfig;
import kr.co.maeum.support.document.DocumentMapper;
import kr.co.maeum.support.document.DocumentService;
import kr.co.maeum.support.identity.IdentityMapper;
import kr.co.maeum.support.identity.PortalPrincipal;
import kr.co.maeum.support.identity.RegistrationService;
import kr.co.maeum.support.ocr.OcrMapper;
import kr.co.maeum.support.ocr.OcrService;
import kr.co.maeum.support.program.ProgramMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;

import java.math.BigDecimal;
import java.util.Map;
import java.util.UUID;

@SpringJUnitConfig(WebConfig.class)
@WebAppConfiguration
class ServiceWorkflowIntegrationTest {
    @Autowired RegistrationService registration;
    @Autowired IdentityMapper identities;
    @Autowired BCryptPasswordEncoder encoder;
    @Autowired ApplicationService workflow;
    @Autowired ApplicationMapper applications;
    @Autowired ProgramMapper programs;
    @Autowired SampleApplicationService samples;
    @Autowired DocumentService files;
    @Autowired DocumentMapper documents;
    @Autowired OcrService ocr;
    @Autowired OcrMapper jobs;

    private String loginId() {
        return "member_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
    }

    private PortalPrincipal member() {
        String login = loginId();
        registration.register(login, "신청자", "Only-test-password-2026!", "Only-test-password-2026!");
        return identities.findLogin(login).principal();
    }

    private long start(PortalPrincipal actor) {
        return workflow.start(actor, programs.findBySeedKey("local-store-environment-v1").getId());
    }

    private byte[] image(String kind) throws Exception {
        try (var stream = getClass().getResourceAsStream("/fixtures/original/" + kind + ".png")) {
            return stream.readAllBytes();
        }
    }

    @Test
    @DisplayName("일반 가입은 신청자 권한과 암호화된 비밀번호만 저장한다")
    void registration_creates_only_normal_applicant() {
        String login = loginId();
        registration.register(
                login, "가상 작성자", "Only-test-password-2026!", "Only-test-password-2026!");
        var actor = identities.findLogin(login).principal();
        assertEquals("APPLICANT", actor.role());
        assertFalse(actor.demo());
        assertTrue(encoder.matches("Only-test-password-2026!", actor.password()));
        assertEquals(
                409,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        registration.register(
                                                login,
                                                "중복",
                                                "Only-test-password-2026!",
                                                "Only-test-password-2026!"))
                        .getStatus());
    }

    @Test
    @DisplayName("새 신청서는 빈 상태이며 예시·서류·OCR을 자동 생성하지 않는다")
    void new_application_is_blank_and_partial_save_is_allowed() {
        var actor = member();
        long id = start(actor);
        var application = applications.find(id);
        assertEquals(
                java.util.List.of("BUSINESS_CERT", "ESTIMATE"), files.editableTypes(actor, id));
        assertNull(application.getBusinessName());
        assertTrue(documents.current(id).isEmpty());
        var form = new ApplicationForm();
        form.setVersion(application.getVersion());
        form.setBusinessName("직접 입력 상호");
        workflow.save(actor, id, form);
        assertEquals("직접 입력 상호", applications.find(id).getBusinessName());
        assertEquals(id, start(actor));
    }

    @Test
    @DisplayName("같은 운영 공간의 다른 신청자도 신청·파일·확인값을 변경할 수 없다")
    void normal_accounts_cannot_access_other_owner() throws Exception {
        var owner = member();
        var other = member();
        long id = start(owner);
        samples.apply(owner, id, applications.find(id).getVersion(), "original");
        files.upload(owner, id, "BUSINESS_CERT", "가상.png", image("business-cert"));
        long documentId = documents.current(id).getFirst().getId();
        assertThrows(BusinessException.class, () -> workflow.view(other, id));
        assertThrows(BusinessException.class, () -> files.editableTypes(other, id));
        assertThrows(BusinessException.class, () -> files.pendingReplacementTypes(other, id));
        assertThrows(BusinessException.class, () -> files.authorizedFile(other, documentId));
        assertThrows(
                BusinessException.class,
                () -> samples.apply(other, id, applications.find(id).getVersion(), "comparison"));
        assertThrows(
                BusinessException.class,
                () -> ocr.applyConfirmed(other, documentId, applications.find(id).getVersion()));
        assertThrows(BusinessException.class, () -> ocr.request(owner, documentId, "SAMPLE"));
    }

    @Test
    @DisplayName("업로드와 직접 확인 후 명시적으로 반영한 값만 신청서에 저장한다")
    void confirmed_values_apply_explicitly_and_snapshot_keeps_business_details() throws Exception {
        var actor = member();
        long id = start(actor);
        samples.apply(actor, id, applications.find(id).getVersion(), "original");
        files.upload(actor, id, "BUSINESS_CERT", "사업자등록증.png", image("business-cert"));
        files.upload(actor, id, "ESTIMATE", "견적서.png", image("estimate"));
        long documentId =
                documents.current(id).stream()
                        .filter(d -> d.getTypeCode().equals("BUSINESS_CERT"))
                        .findFirst()
                        .orElseThrow()
                        .getId();
        assertNull(jobs.findDocumentJob(documentId));
        ocr.confirm(
                actor,
                documentId,
                Map.of(
                        "businessName",
                        "수정한 가상상점",
                        "businessNo",
                        "0000000000",
                        "representative",
                        "가상대표",
                        "openedOn",
                        "2024-01-01"));
        assertEquals("가상상점", applications.find(id).getBusinessName());
        long version = applications.find(id).getVersion();
        ocr.applyConfirmed(actor, documentId, version);
        assertEquals("수정한 가상상점", applications.find(id).getBusinessName());
        assertThrows(BusinessException.class, () -> ocr.applyConfirmed(actor, documentId, version));
        assertEquals(new BigDecimal("3300000"), applications.find(id).getTotalProjectCost());
        long revision =
                workflow.submit(
                        actor,
                        id,
                        applications.find(id).getVersion(),
                        UUID.randomUUID().toString(),
                        null);
        assertEquals(id, start(actor));
        var practice =
                new PortalPrincipal(
                        actor.id(),
                        actor.workspaceId(),
                        "applicant",
                        actor.password(),
                        actor.displayName(),
                        actor.role(),
                        false);
        try (var executor = java.util.concurrent.Executors.newFixedThreadPool(2)) {
            var first = executor.submit(() -> start(practice));
            var second = executor.submit(() -> start(practice));
            long freshId = first.get();
            assertEquals(freshId, second.get().longValue());
            assertNotEquals(id, freshId);
            assertEquals("DRAFT", applications.find(freshId).getStatus());
            assertNull(applications.find(freshId).getBusinessName());
            assertTrue(documents.current(freshId).isEmpty());
            assertEquals(freshId, start(practice));
            assertEquals("SUBMITTED", applications.find(id).getStatus());
            assertEquals(1, applications.revisions(id).size());
        }
        String snapshot = applications.revisionDetail(id, revision).get("snapshot").toString();
        assertTrue(snapshot.contains("도움로 123"));
        assertTrue(snapshot.contains("selfContribution"));
        assertTrue(snapshot.contains("totalProjectCost"));
        assertThrows(
                BusinessException.class,
                () -> ocr.applyConfirmed(actor, documentId, applications.find(id).getVersion()));
    }

    @Test
    @DisplayName("사업장 정보 누락이나 음수 자부담은 최종 제출할 수 없다")
    void submission_requires_business_details_and_valid_contribution() {
        var actor = member();
        long id = start(actor);
        samples.apply(actor, id, applications.find(id).getVersion(), "original");
        var form = new ApplicationForm();
        BeanUtils.copyProperties(applications.find(id), form);
        form.setSelfContribution(new BigDecimal("-1"));
        assertThrows(BusinessException.class, () -> workflow.save(actor, id, form));
        BeanUtils.copyProperties(applications.find(id), form);
        form.setAddress("");
        workflow.save(actor, id, form);
        assertThrows(
                BusinessException.class,
                () ->
                        workflow.submit(
                                actor,
                                id,
                                applications.find(id).getVersion(),
                                UUID.randomUUID().toString(),
                                null));
    }

    @Test
    @DisplayName("일반 계정의 AI 한도는 다른 신청자의 사용량과 분리한다")
    void ai_daily_quota_is_separate_for_normal_users() throws Exception {
        System.setProperty("OCR_LIVE_ENABLED", "true");
        System.setProperty("OCR_AI_ENABLED", "true");
        System.setProperty("AI_LIMIT_PER_DAY", "1");
        System.setProperty("AI_LIMIT_GLOBAL_PER_DAY", "100000");
        try {
            var first = member();
            var second = member();
            long firstId = start(first);
            long secondId = start(second);
            files.upload(first, firstId, "BUSINESS_CERT", "가상.png", image("business-cert"));
            ocr.request(first, documents.current(firstId).getFirst().getId(), "LIVE_AI");
            files.upload(first, firstId, "BUSINESS_CERT", "다시첨부.png", image("business-cert"));
            long nextDocument = documents.current(firstId).getFirst().getId();
            assertEquals(
                    429,
                    assertThrows(
                                    BusinessException.class,
                                    () -> ocr.request(first, nextDocument, "LIVE_AI"))
                            .getStatus());
            files.upload(second, secondId, "BUSINESS_CERT", "가상.png", image("business-cert"));
            long secondDocument = documents.current(secondId).getFirst().getId();
            ocr.request(second, secondDocument, "LIVE_AI");
            assertNotNull(jobs.findModeJob(secondDocument, "LIVE_AI"));
        } finally {
            System.clearProperty("OCR_LIVE_ENABLED");
            System.clearProperty("OCR_AI_ENABLED");
            System.clearProperty("AI_LIMIT_PER_DAY");
            System.clearProperty("AI_LIMIT_GLOBAL_PER_DAY");
        }
    }
}
