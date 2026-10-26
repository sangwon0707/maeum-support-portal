package kr.co.maeum.support.intake;

import static org.junit.jupiter.api.Assertions.*;

import kr.co.maeum.support.application.ApplicationService;
import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.config.WebConfig;
import kr.co.maeum.support.identity.IdentityMapper;
import kr.co.maeum.support.identity.PortalPrincipal;
import kr.co.maeum.support.identity.RegistrationService;
import kr.co.maeum.support.program.ProgramMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

import javax.sql.DataSource;

@SpringJUnitConfig(WebConfig.class)
@WebAppConfiguration
@Transactional
class PreparedIntakeIntegrationTest {
    @Autowired IntakeService service;
    @Autowired IntakeMapper jobs;
    @Autowired PreparedIntakeWorker worker;
    @Autowired PreparedIntakeCatalog catalog;
    @Autowired IntakeStorage storage;
    @Autowired ApplicationService applications;
    @Autowired ProgramMapper programs;
    @Autowired RegistrationService registration;
    @Autowired IdentityMapper identities;
    @Autowired DataSource dataSource;

    private PortalPrincipal member() {
        String login = "prepared_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        registration.register(
                login, "가상 신청자", "Only-test-password-2026!", "Only-test-password-2026!");
        return identities.findLogin(login).principal();
    }

    private long app(PortalPrincipal actor) {
        return applications.start(
                actor, programs.findBySeedKey("local-store-environment-v1").getId());
    }

    private byte[] scan() throws Exception {
        try (var input = getClass().getResourceAsStream("/fixtures/digital/application-scan.pdf")) {
            return input.readAllBytes();
        }
    }

    private long upload(PortalPrincipal actor, long app, byte[] bytes) {
        return service.upload(
                actor,
                app,
                "BUNDLE",
                List.of(new IntakeService.Upload("검증.pdf", "AUTO", bytes)),
                UUID.randomUUID().toString());
    }

    @Test
    @org.springframework.transaction.annotation.Transactional(
            propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    @DisplayName("실제 작업 만료 수거는 저장 중인 데모 작업의 행 잠금을 기다리지 않는다")
    void live_expiry_does_not_lock_prepared_result_rows() throws Exception {
        var actor = member();
        long app = app(actor);
        long id = upload(actor, app, scan());
        service.startAnalysis(actor, app, id, "PREPARED", false);
        try (var connection = dataSource.getConnection()) {
            connection.setAutoCommit(false);
            try (var lock =
                    connection.prepareStatement(
                            "SELECT id FROM document_intake WHERE id=? FOR UPDATE")) {
                lock.setLong(1, id);
                lock.executeQuery().close();
                try (var executor = java.util.concurrent.Executors.newSingleThreadExecutor()) {
                    var expiry = executor.submit(() -> jobs.expireInterrupted());
                    try {
                        assertDoesNotThrow(
                                () -> expiry.get(2, java.util.concurrent.TimeUnit.SECONDS));
                    } finally {
                        // 회귀가 발생해도 작업 스레드를 기다리기 전에 행 잠금을 해제한다.
                        connection.rollback();
                    }
                }
            } finally {
                connection.rollback();
            }
        } finally {
            new JdbcTemplate(dataSource)
                    .update("UPDATE document_intake SET status='FAILED' WHERE id=?", id);
        }
    }

    @Test
    @DisplayName("준비된 검토는 업로드만으로 시작하지 않고 단계 표시 후 새 원본에 연결한 결과를 만든다")
    void prepared_review_waits_for_request_and_restores_real_outputs() throws Exception {
        var actor = member();
        long app = app(actor);
        long id = upload(actor, app, scan());
        assertTrue(service.preparedAvailable(jobs.find(id)));
        worker.runPending();
        assertEquals("UPLOADED", jobs.find(id).get("status"));
        service.startAnalysis(actor, app, id, "PREPARED", false);
        assertEquals("PREPARED", jobs.find(id).get("execution_mode"));
        assertFalse(jobs.pending().contains(id));
        assertEquals("PROCESSING", jobs.find(id).get("status"));
        new JdbcTemplate(dataSource)
                .update(
                        "UPDATE document_intake SET"
                            + " replay_started_at=TIMESTAMPADD(SECOND,-20,UTC_TIMESTAMP(6)) WHERE"
                            + " id=?",
                        id);
        worker.runPending();
        var job = jobs.find(id);
        assertEquals("READY", job.get("status"));
        var result = service.result(job);
        assertEquals("PREPARED", result.get("executionMode"));
        var pages = (List<java.util.Map<String, Object>>) result.get("pages");
        assertEquals(6, pages.size());
        assertEquals(
                jobs.sources(id).getFirst().get("id").toString(),
                pages.getFirst().get("sourceId").toString());
        assertEquals("검증.pdf", pages.getFirst().get("sourceName"));
        assertTrue(
                java.nio.file.Files.exists(
                        storage.resolve(pages.getFirst().get("previewKey").toString())));
    }

    @Test
    @DisplayName("내용이 다른 파일에 사전 결과를 연결하지 않으며 실제 인식은 시간 확인을 요구한다")
    void unknown_content_and_unacknowledged_live_requests_are_rejected() throws Exception {
        var actor = member();
        long app = app(actor);
        long unknown = upload(actor, app, ("%PDF-" + UUID.randomUUID()).getBytes());
        assertFalse(service.preparedAvailable(jobs.find(unknown)));
        assertEquals(
                422,
                assertThrows(
                                BusinessException.class,
                                () -> service.startAnalysis(actor, app, unknown, "PREPARED", false))
                        .getStatus());
        long known = upload(actor, app, scan());
        assertEquals(
                422,
                assertThrows(
                                BusinessException.class,
                                () -> service.startAnalysis(actor, app, known, "LIVE", false))
                        .getStatus());
        var other = member();
        assertThrows(
                BusinessException.class,
                () -> service.startAnalysis(other, app, known, "PREPARED", false));
        assertEquals("UPLOADED", jobs.find(known).get("status"));
    }

    @Test
    @DisplayName("모든 사전 문서는 실제 인식 출처와 원본 해시를 가지며 다른 이름으로도 동일 바이트를 식별한다")
    void provided_documents_have_traceable_real_ocr_results() throws Exception {
        var actor = member();
        long app = app(actor);
        for (String code :
                List.of(
                        "original",
                        "comparison",
                        "digital",
                        "energy",
                        "access",
                        "online",
                        "safety")) {
            byte[] bytes;
            try (var input =
                    getClass().getResourceAsStream("/fixtures/" + code + "/application-scan.pdf")) {
                bytes = input.readAllBytes();
            }
            long id = upload(actor, app, bytes);
            assertEquals(code, catalog.match("BUNDLE", jobs.sources(id)));
        }
    }

    @Test
    @DisplayName("실제 재검토는 확인 후 별도 작업으로 생성하고 원본 공유·중복 요청·기존 결과를 보존한다")
    void live_retest_preserves_prepared_result_and_reuses_upload_identity() throws Exception {
        var actor = member();
        long app = app(actor);
        long original = upload(actor, app, scan());
        service.startAnalysis(actor, app, original, "PREPARED", false);
        new JdbcTemplate(dataSource)
                .update(
                        "UPDATE document_intake SET"
                            + " replay_started_at=TIMESTAMPADD(SECOND,-20,UTC_TIMESTAMP(6)) WHERE"
                            + " id=?",
                        original);
        worker.runPending();
        String key = UUID.randomUUID().toString();
        assertThrows(
                BusinessException.class,
                () -> service.requestLiveTest(actor, app, original, key, false));
        String previous = System.getProperty("OCR_LIVE_ENABLED");
        try {
            System.setProperty("OCR_LIVE_ENABLED", "true");
            long live = service.requestLiveTest(actor, app, original, key, true);
            assertNotEquals(original, live);
            assertEquals("LIVE", jobs.find(live).get("execution_mode"));
            assertEquals("QUEUED", jobs.find(live).get("status"));
            assertEquals("READY", jobs.find(original).get("status"));
            assertEquals(
                    jobs.sources(original).getFirst().get("storage_key"),
                    jobs.sources(live).getFirst().get("storage_key"));
            assertEquals(live, service.requestLiveTest(actor, app, original, key, true));
        } finally {
            if (previous == null) System.clearProperty("OCR_LIVE_ENABLED");
            else System.setProperty("OCR_LIVE_ENABLED", previous);
        }
    }
}
