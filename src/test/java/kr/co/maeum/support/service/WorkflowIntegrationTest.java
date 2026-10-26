package kr.co.maeum.support.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.config.WebConfig;
import kr.co.maeum.support.dto.ApplicationForm;
import kr.co.maeum.support.mapper.ApplicationMapper;
import kr.co.maeum.support.mapper.DocumentMapper;
import kr.co.maeum.support.mapper.IdentityMapper;
import kr.co.maeum.support.mapper.OcrMapper;
import kr.co.maeum.support.security.PortalPrincipal;
import kr.co.maeum.support.service.policy.ImageValidator;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.test.web.servlet.request.MockMvcRequestBuilders;
import org.springframework.test.web.servlet.result.MockMvcResultMatchers;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

import java.nio.charset.StandardCharsets;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Executors;

import javax.sql.DataSource;

@SpringJUnitConfig(WebConfig.class)
@WebAppConfiguration
class WorkflowIntegrationTest {
    @Autowired DemoService demos;
    @Autowired ApplicationService workflow;
    @Autowired ApplicationMapper applications;
    @Autowired DocumentMapper documents;
    @Autowired DocumentService files;
    @Autowired ReviewService reviews;
    @Autowired IdentityMapper identities;
    @Autowired DataSource dataSource;
    @Autowired ImageValidator validator;
    @Autowired OcrMapper jobs;
    @Autowired OcrService ocr;

    private PortalPrincipal demo(String scenario) {
        return demos.start(
                scenario,
                UUID.randomUUID().toString().replace("-", "")
                        + UUID.randomUUID().toString().replace("-", ""));
    }

    private long id(PortalPrincipal actor) {
        return new JdbcTemplate(dataSource)
                .queryForObject(
                        "SELECT id FROM application WHERE workspace_id=?",
                        Long.class,
                        actor.workspaceId());
    }

    private long version(long id) {
        return applications.find(id).getVersion();
    }

    @Test
    @DisplayName("정상 제출과 재요청은 제출본을 한 번만 만든다")
    void submit_repeated_key_returns_same_revision() {
        var actor = demo("NORMAL");
        long id = id(actor);
        String key = UUID.randomUUID().toString();
        long version = version(id);
        long first = workflow.submit(actor, id, version, key, null);
        long second = workflow.submit(actor, id, version, key, null);
        assertEquals(first, second);
        assertEquals(1, applications.revisions(id).size());
        assertEquals("SUBMITTED", applications.find(id).getStatus());
        var controller =
                webContext.getBean(kr.co.maeum.support.controller.ApplicationController.class);
        assertEquals(
                "redirect:/applications/" + id,
                controller.edit(actor, id, new org.springframework.ui.ExtendedModelMap()));
    }

    @Test
    @DisplayName("다른 체험 공간의 신청·파일·OCR에 접근할 수 없다")
    void isolated_workspace_rejects_other_actor() {
        var first = demo("NORMAL");
        var second = demo("NORMAL");
        long id = id(first);
        long documentId = documents.current(id).getFirst().getId();
        assertEquals(
                403,
                assertThrows(BusinessException.class, () -> workflow.view(second, id)).getStatus());
        assertThrows(BusinessException.class, () -> files.authorizedFile(second, documentId));
        assertThrows(BusinessException.class, () -> ocr.request(second, documentId));
    }

    @Test
    @DisplayName("견적서가 빠진 신청은 제출되지 않는다")
    void submit_requires_all_documents() {
        var actor = demo("MISSING");
        long id = id(actor);
        assertEquals(
                422,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        workflow.submit(
                                                actor,
                                                id,
                                                version(id),
                                                UUID.randomUUID().toString(),
                                                null))
                        .getStatus());
        assertEquals("DRAFT", applications.find(id).getStatus());
        assertEquals(0, applications.revisions(id).size());
    }

    @Test
    @DisplayName("보완 서류를 교체하면 새 제출본을 만들고 이전 첨부를 보존한다")
    void supplement_requires_new_document_and_preserves_revision() {
        var actor = demo("SUPPLEMENT");
        long id = id(actor);
        assertEquals(java.util.List.of("ESTIMATE"), files.editableTypes(actor, id));
        assertEquals(java.util.List.of("ESTIMATE"), files.pendingReplacementTypes(actor, id));
        long originalDocument =
                documents.current(id).stream()
                        .filter(d -> d.getTypeCode().equals("ESTIMATE"))
                        .findFirst()
                        .orElseThrow()
                        .getId();
        assertThrows(
                BusinessException.class,
                () ->
                        workflow.submit(
                                actor, id, version(id), UUID.randomUUID().toString(), "보완했습니다."));
        files.attachFixture(actor, id, "ESTIMATE");
        assertEquals(java.util.List.of(), files.pendingReplacementTypes(actor, id));
        workflow.submit(actor, id, version(id), UUID.randomUUID().toString(), "세부 항목을 보완했습니다.");
        assertEquals(java.util.List.of(), files.editableTypes(actor, id));
        assertEquals(2, applications.revisions(id).size());
        assertNotNull(documents.find(originalDocument));
        assertEquals("SUBMITTED", applications.find(id).getStatus());
        assertEquals(
                2,
                new JdbcTemplate(dataSource)
                        .queryForObject(
                                "SELECT COUNT(*) FROM revision_document rd JOIN"
                                        + " application_revision r ON r.id=rd.revision_id WHERE"
                                        + " r.application_id=? AND r.revision_no=1",
                                Integer.class,
                                id));
    }

    @Test
    @DisplayName("보완 요청 범위를 벗어난 필드·서류 변경은 거절한다")
    void supplement_rejects_unrequested_changes() {
        var actor = demo("SUPPLEMENT");
        long id = id(actor);
        var form = new ApplicationForm();
        BeanUtils.copyProperties(applications.find(id), form);
        form.setBusinessName("무단변경");
        assertThrows(BusinessException.class, () -> workflow.save(actor, id, form));
        assertThrows(
                BusinessException.class, () -> files.attachFixture(actor, id, "BUSINESS_CERT"));
    }

    @Test
    @DisplayName("옛 버전 검토 요청은 확정 결과를 덮어쓰지 않는다")
    void decision_rejects_stale_version() {
        var actor = demo("NORMAL");
        long id = id(actor);
        workflow.submit(actor, id, version(id), UUID.randomUUID().toString(), null);
        var staff = demos.switchRole(actor, "STAFF");
        reviews.start(staff, id, version(id));
        long expected = version(id);
        reviews.decide(staff, id, expected, "APPROVED", "서류 확인 완료", true);
        assertEquals(
                409,
                assertThrows(
                                BusinessException.class,
                                () ->
                                        reviews.decide(
                                                staff, id, expected, "REJECTED", "중복 결과", true))
                        .getStatus());
        assertEquals("APPROVED", applications.find(id).getStatus());
    }

    @Test
    @DisplayName("체험 역할 전환으로 관리자를 얻을 수 없다")
    void demo_cannot_escalate_to_admin() {
        var actor = demo("NORMAL");
        assertThrows(BusinessException.class, () -> demos.switchRole(actor, "ADMIN"));
    }

    @Test
    @DisplayName("PNG 이름의 일반 텍스트는 이미지로 저장하지 않는다")
    void rejects_spoofed_image() {
        assertThrows(
                BusinessException.class,
                () -> validator.normalize("not a png".getBytes(StandardCharsets.UTF_8)));
    }

    @Test
    @DisplayName("만료 공간의 세션은 다시 업무를 조회할 수 없다")
    void expired_workspace_revokes_access() {
        var actor = demo("NORMAL");
        long id = id(actor);
        demos.expire(actor);
        assertThrows(BusinessException.class, () -> workflow.view(actor, id));
    }

    @Test
    @DisplayName("교체된 서류의 OCR 결과에 확인값을 저장할 수 없다")
    void old_document_cannot_be_confirmed() {
        var actor = demo("NORMAL");
        long id = id(actor);
        long documentId =
                documents.current(id).stream()
                        .filter(d -> d.getTypeCode().equals("BUSINESS_CERT"))
                        .findFirst()
                        .orElseThrow()
                        .getId();
        files.attachFixture(actor, id, "BUSINESS_CERT");
        assertThrows(BusinessException.class, () -> ocr.confirm(actor, documentId, Map.of()));
    }

    @Test
    @DisplayName("보완 기한이 지난 경우 재제출을 차단한다")
    void expired_supplement_cannot_be_submitted() {
        var actor = demo("SUPPLEMENT");
        long id = id(actor);
        new JdbcTemplate(dataSource)
                .update(
                        "UPDATE supplement_request SET"
                                + " due_at=TIMESTAMPADD(MINUTE,-1,UTC_TIMESTAMP(6)) WHERE"
                                + " application_id=?",
                        id);
        assertThrows(
                BusinessException.class,
                () -> workflow.submit(actor, id, version(id), UUID.randomUUID().toString(), "보완"));
    }

    @Test
    @DisplayName("동시에 같은 제출 키를 사용해도 제출본은 한 개다")
    void concurrent_submit_is_idempotent() throws Exception {
        var actor = demo("NORMAL");
        long id = id(actor);
        long expected = version(id);
        String key = UUID.randomUUID().toString();
        try (var executor = Executors.newVirtualThreadPerTaskExecutor()) {
            var first = executor.submit(() -> workflow.submit(actor, id, expected, key, null));
            var second = executor.submit(() -> workflow.submit(actor, id, expected, key, null));
            assertEquals(first.get(), second.get());
        }
        assertEquals(1, applications.revisions(id).size());
    }

    @Autowired WebApplicationContext webContext;

    @Test
    @DisplayName("인증됐어도 CSRF 없는 변경 요청은 차단한다")
    void security_rejects_missing_csrf() throws Exception {
        var actor = demo("NORMAL");
        var mvc =
                MockMvcBuilders.webAppContextSetup(webContext)
                        .apply(
                                org.springframework.security.test.web.servlet.setup
                                        .SecurityMockMvcConfigurers.springSecurity())
                        .build();
        mvc.perform(
                        MockMvcRequestBuilders.post("/applications")
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors.user(actor))
                                .param("programId", "1"))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
        mvc.perform(
                        MockMvcRequestBuilders.get("/admin")
                                .with(
                                        org.springframework.security.test.web.servlet.request
                                                .SecurityMockMvcRequestPostProcessors.user(actor)))
                .andExpect(MockMvcResultMatchers.status().isForbidden());
    }

    @Test
    @DisplayName("담당자를 이관하면 이전 담당자는 즉시 조회할 수 없다")
    void reassignment_revokes_previous_staff() {
        var actor = demo("NORMAL");
        long id = id(actor);
        var oldStaff = demos.switchRole(actor, "STAFF");
        demos.createUser(
                actor.workspaceId(),
                "new-staff-" + UUID.randomUUID(),
                "새 담당자",
                "STAFF",
                "temporary-test-password");
        long nextStaff =
                new JdbcTemplate(dataSource)
                        .queryForObject(
                                "SELECT MAX(id) FROM app_user WHERE workspace_id=?",
                                Long.class,
                                actor.workspaceId());
        applications.assign(
                Map.of(
                        "applicationId",
                        id,
                        "staffId",
                        nextStaff,
                        "assignedBy",
                        nextStaff,
                        "reason",
                        "이관 테스트"));
        assertThrows(BusinessException.class, () -> workflow.view(oldStaff, id));
    }

    @Autowired kr.co.maeum.support.scheduler.OcrWorker worker;

    @Test
    @DisplayName("마지막 OCR 시도 도중 종료되어도 만료 후 실패 상태로 회수한다")
    void exhausted_lease_is_not_left_running() {
        var actor = demo("NORMAL");
        long applicationId = id(actor);
        long documentId =
                documents.current(applicationId).stream()
                        .filter(document -> document.getTypeCode().equals("BUSINESS_CERT"))
                        .findFirst()
                        .orElseThrow()
                        .getId();
        new JdbcTemplate(dataSource)
                .update(
                        "UPDATE ocr_job SET"
                            + " status='RUNNING',attempts=3,lease_until=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP(6))"
                            + " WHERE document_id=?",
                        documentId);
        worker.runPending();
        assertEquals("FAILED", jobs.findDocumentJob(documentId).getStatus());
        assertEquals("LEASE_EXPIRED", jobs.findDocumentJob(documentId).getErrorCode());
    }

    @Test
    @DisplayName("실제 OCR이 비활성화된 서버는 샘플 요청만 허용한다")
    void disabled_live_ocr_rejects_request_without_job() {
        var actor = demo("NORMAL");
        long documentId =
                documents.current(id(actor)).stream()
                        .filter(d -> d.getTypeCode().equals("BUSINESS_CERT"))
                        .findFirst()
                        .orElseThrow()
                        .getId();
        assertThrows(BusinessException.class, () -> ocr.request(actor, documentId, "LIVE"));
        assertEquals(null, jobs.findModeJob(documentId, "LIVE"));
    }

    @Test
    @DisplayName("같은 서류의 OCR 모드를 분리하고 같은 모드 요청은 한 번만 저장한다")
    void live_mode_does_not_overwrite_sample_job() {
        System.setProperty("OCR_LIVE_ENABLED", "true");
        try {
            var actor = demo("NORMAL");
            long documentId =
                    documents.current(id(actor)).stream()
                            .filter(d -> d.getTypeCode().equals("BUSINESS_CERT"))
                            .findFirst()
                            .orElseThrow()
                            .getId();
            ocr.request(actor, documentId, "LIVE");
            long first = jobs.findModeJob(documentId, "LIVE").getId();
            ocr.request(actor, documentId, "LIVE");
            assertEquals(first, jobs.findModeJob(documentId, "LIVE").getId());
            assertNotNull(jobs.findModeJob(documentId, "SAMPLE"));
        } finally {
            System.clearProperty("OCR_LIVE_ENABLED");
        }
    }

    @Test
    @DisplayName("AI 한도를 소진하면 작업을 저장하지 않고 로컬 OCR은 계속 허용한다")
    void ai_quota_rejection_keeps_local_ocr_available() {
        System.setProperty("OCR_LIVE_ENABLED", "true");
        System.setProperty("OCR_AI_ENABLED", "true");
        System.setProperty("AI_LIMIT_GLOBAL_PER_DAY", "0");
        try {
            var actor = demo("NORMAL");
            long documentId =
                    documents.current(id(actor)).stream()
                            .filter(d -> d.getTypeCode().equals("BUSINESS_CERT"))
                            .findFirst()
                            .orElseThrow()
                            .getId();
            assertEquals(
                    429,
                    assertThrows(
                                    BusinessException.class,
                                    () -> ocr.request(actor, documentId, "LIVE_AI"))
                            .getStatus());
            assertEquals(null, jobs.findModeJob(documentId, "LIVE_AI"));
            ocr.request(actor, documentId, "LIVE");
            assertNotNull(jobs.findModeJob(documentId, "LIVE"));
        } finally {
            System.clearProperty("OCR_LIVE_ENABLED");
            System.clearProperty("OCR_AI_ENABLED");
            System.clearProperty("AI_LIMIT_GLOBAL_PER_DAY");
        }
    }

    @Test
    @DisplayName("임대를 잃은 이전 작업자는 늦은 결과를 완료 처리할 수 없다")
    void stale_claim_token_cannot_finish_job() {
        var actor = demo("NORMAL");
        long documentId =
                documents.current(id(actor)).stream()
                        .filter(d -> d.getTypeCode().equals("BUSINESS_CERT"))
                        .findFirst()
                        .orElseThrow()
                        .getId();
        long jobId = jobs.findDocumentJob(documentId).getId();
        new JdbcTemplate(dataSource)
                .update(
                        "UPDATE ocr_job SET status='RUNNING',claim_token='new-owner' WHERE id=?",
                        jobId);
        assertEquals(0, jobs.finish(jobId, "old-owner"));
        assertEquals("RUNNING", jobs.findDocumentJob(documentId).getStatus());
    }

    @Test
    @DisplayName("외부 AI 작업의 임대가 만료되면 중복 청구를 피하려고 재실행하지 않는다")
    void expired_ai_claim_is_failed_without_retry() {
        var actor = demo("NORMAL");
        long documentId =
                documents.current(id(actor)).stream()
                        .filter(d -> d.getTypeCode().equals("BUSINESS_CERT"))
                        .findFirst()
                        .orElseThrow()
                        .getId();
        jobs.insert(documentId, "LIVE_AI", "lease-test");
        var job = jobs.findModeJob(documentId, "LIVE_AI");
        new JdbcTemplate(dataSource)
                .update(
                        "UPDATE ocr_job SET"
                            + " status='RUNNING',attempts=1,lease_until=TIMESTAMPADD(SECOND,-1,UTC_TIMESTAMP(6))"
                            + " WHERE id=?",
                        job.getId());
        jobs.expireExhausted();
        assertEquals("FAILED", jobs.findModeJob(documentId, "LIVE_AI").getStatus());
    }
}
