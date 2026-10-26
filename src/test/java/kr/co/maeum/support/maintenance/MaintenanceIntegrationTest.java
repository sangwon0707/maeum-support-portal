package kr.co.maeum.support.maintenance;

import static org.junit.jupiter.api.Assertions.*;

import kr.co.maeum.support.application.ApplicationService;
import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.config.WebConfig;
import kr.co.maeum.support.identity.IdentityMapper;
import kr.co.maeum.support.identity.PortalPrincipal;
import kr.co.maeum.support.identity.RegistrationService;
import kr.co.maeum.support.intake.IntakeMapper;
import kr.co.maeum.support.intake.IntakeResultCache;
import kr.co.maeum.support.intake.IntakeService;
import kr.co.maeum.support.intake.IntakeStorage;
import kr.co.maeum.support.program.ProgramMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.nio.file.Files;
import java.nio.file.attribute.FileTime;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.sql.DataSource;

@SpringJUnitConfig(WebConfig.class)
@WebAppConfiguration
class MaintenanceIntegrationTest {
    @Autowired MaintenanceService maintenance;
    @Autowired IntakeStorage storage;
    @Autowired IntakeService intake;
    @Autowired IntakeMapper jobs;
    @Autowired IntakeResultCache cache;
    @Autowired ContentFileStore files;
    @Autowired ApplicationService applications;
    @Autowired RegistrationService registration;
    @Autowired IdentityMapper identities;
    @Autowired ProgramMapper programs;
    @Autowired DataSource dataSource;

    private PortalPrincipal member() {
        String login = "reset_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        registration.register(
                login, "가상 신청자", "Only-test-password-2026!", "Only-test-password-2026!");
        return identities.findLogin(login).principal();
    }

    private long application(PortalPrincipal actor) {
        return applications.start(
                actor, programs.findBySeedKey("local-store-environment-v1").getId());
    }

    private long upload(PortalPrincipal actor, long id, byte[] bytes) {
        return intake.upload(
                actor,
                id,
                "BUNDLE",
                List.of(new IntakeService.Upload("신청.pdf", "AUTO", bytes)),
                UUID.randomUUID().toString());
    }

    private byte[] pdf() {
        return ("%PDF-1.4\n" + UUID.randomUUID()).getBytes(java.nio.charset.StandardCharsets.UTF_8);
    }

    private String login(PortalPrincipal actor) {
        return new JdbcTemplate(dataSource)
                .queryForObject(
                        "SELECT login_id FROM app_user WHERE id=?", String.class, actor.id());
    }

    @Test
    @DisplayName("동일 내용은 한 파일로 공유하고 한 신청 삭제 후에도 다른 신청 파일을 보존한다")
    void repeated_uploads_share_bytes_and_cleanup_preserves_other_reference() throws Exception {
        var first = member();
        var second = member();
        long firstApp = application(first), secondApp = application(second);
        byte[] bytes = pdf();
        long firstJob = upload(first, firstApp, bytes),
                secondJob = upload(second, secondApp, bytes);
        String key = jobs.sources(firstJob).getFirst().get("storage_key").toString();
        assertEquals(key, jobs.sources(secondJob).getFirst().get("storage_key"));
        assertEquals(64 + 4, key.length());
        Files.setLastModifiedTime(
                storage.resolve(key), FileTime.from(Instant.now().minusSeconds(7200)));
        assertEquals(1, maintenance.resetBefore(login(first), LocalDateTime.now().plusDays(1)));
        maintenance.collectOrphans();
        assertTrue(Files.exists(storage.resolve(key)));
        assertNotNull(jobs.find(secondJob));
        assertEquals(1, maintenance.resetBefore(login(second), LocalDateTime.now().plusDays(1)));
        maintenance.collectOrphans();
        assertFalse(Files.exists(storage.resolve(key)));
        assertEquals(0, maintenance.resetBefore(login(second), LocalDateTime.now().plusDays(1)));
    }

    @Test
    @Transactional
    @DisplayName("기준 시각 이후 신청과 진행 중 OCR은 보존하고 완료 후 이전 신청을 삭제한다")
    void reset_skips_new_and_active_jobs_then_deletes_finished_jobs() {
        var actor = member();
        long app = application(actor);
        long job = upload(actor, app, pdf());
        var sql = new JdbcTemplate(dataSource);
        var cutoff = LocalDateTime.now().minusDays(1);
        assertEquals(0, maintenance.resetBefore(login(actor), cutoff));
        sql.update("UPDATE application SET created_at=? WHERE id=?", cutoff.minusHours(1), app);
        jobs.enqueue(job);
        assertEquals(0, maintenance.resetBefore(login(actor), cutoff));
        sql.update("UPDATE document_intake SET status='FAILED' WHERE id=?", job);
        assertEquals(1, maintenance.resetBefore(login(actor), cutoff));
        assertNull(jobs.find(job));
        assertEquals(
                1,
                sql.queryForObject(
                        "SELECT COUNT(*) FROM app_user WHERE id=?", Integer.class, actor.id()));
    }

    @Test
    @Transactional
    @DisplayName("실제 엔진 결과 캐시는 소유자와 처리 버전을 구분하고 새 원본 식별자로 연결한다")
    void cache_rebinds_sources_and_isolates_owner_and_pipeline() throws Exception {
        var actor = member();
        long app = application(actor);
        byte[] bytes = pdf();
        long first = upload(actor, app, bytes);
        var sources = jobs.sources(first);
        String preview = storage.save(new byte[] {1, 2, 3}, false);
        cache.remember(
                first,
                sources,
                Map.of(
                        "pages",
                        List.of(
                                Map.of(
                                        "sourceId",
                                        sources.getFirst().get("id"),
                                        "sourceName",
                                        "이전.pdf",
                                        "previewKey",
                                        preview)),
                        "fields",
                        Map.of("businessName", "원본 추출값")));
        long second = upload(actor, app, bytes);
        var reused =
                new com.fasterxml.jackson.databind.ObjectMapper()
                        .readTree(cache.reuse(second, jobs.sources(second)));
        assertEquals(
                ((Number) jobs.sources(second).getFirst().get("id")).longValue(),
                reused.path("pages").get(0).path("sourceId").asLong());
        assertEquals("신청.pdf", reused.path("pages").get(0).path("sourceName").asText());
        var other = member();
        long otherJob = upload(other, application(other), bytes);
        assertNull(cache.reuse(otherJob, jobs.sources(otherJob)));
        String prior = System.getProperty("DOCUMENT_PIPELINE_VERSION");
        try {
            System.setProperty("DOCUMENT_PIPELINE_VERSION", "different-pipeline");
            assertNull(cache.reuse(second, jobs.sources(second)));
        } finally {
            if (prior == null) System.clearProperty("DOCUMENT_PIPELINE_VERSION");
            else System.setProperty("DOCUMENT_PIPELINE_VERSION", prior);
        }
    }

    @Test
    @DisplayName("저장 한도는 새 내용만 차단하고 이미 저장된 동일 파일의 재사용은 허용한다")
    void quota_rejects_new_bytes_but_allows_reuse() {
        byte[] bytes = pdf();
        String key = storage.save(bytes, true);
        String previous = System.getProperty("FILE_MAX_BYTES");
        try {
            System.setProperty("FILE_MAX_BYTES", "1");
            assertEquals(key, storage.save(bytes, true));
            assertEquals(
                    507,
                    assertThrows(BusinessException.class, () -> storage.save(pdf(), true))
                            .getStatus());
        } finally {
            if (previous == null) System.clearProperty("FILE_MAX_BYTES");
            else System.setProperty("FILE_MAX_BYTES", previous);
        }
    }

    @Test
    @Transactional
    @DisplayName("제출본·보완·검토·OCR 외래키 전체를 자식부터 정리하고 공고와 계정은 남긴다")
    void reset_removes_all_related_records_without_deleting_account_or_program() {
        var actor = member();
        long app = application(actor);
        var sql = new JdbcTemplate(dataSource);
        String key = files.save(pdf(), false, ".png");
        sql.update(
                "INSERT INTO"
                    + " document(application_id,type_code,version,storage_key,original_name,mime_type,size_bytes,sha256,created_by)"
                    + " VALUES(?,'BUSINESS_CERT',1,?,'등록증.png','image/png',10,?,?)",
                app,
                key,
                "a".repeat(64),
                actor.id());
        long doc =
                sql.queryForObject(
                        "SELECT id FROM document WHERE application_id=?", Long.class, app);
        sql.update(
                "INSERT INTO application_revision(application_id,revision_no,snapshot,submitted_by)"
                        + " VALUES(?,1,'{}',?)",
                app,
                actor.id());
        long revision =
                sql.queryForObject(
                        "SELECT id FROM application_revision WHERE application_id=?",
                        Long.class,
                        app);
        sql.update("INSERT INTO revision_document VALUES(?,?)", revision, doc);
        sql.update(
                "INSERT INTO"
                    + " submission_command(application_id,idempotency_key,payload_hash,revision_id)"
                    + " VALUES(?,'reset-test',?,?)",
                app,
                "b".repeat(64),
                revision);
        sql.update("INSERT INTO ocr_job(document_id,status) VALUES(?,'SUCCEEDED')", doc);
        long job =
                sql.queryForObject("SELECT id FROM ocr_job WHERE document_id=?", Long.class, doc);
        sql.update("INSERT INTO ocr_result(job_id,extracted_fields) VALUES(?,'{}')", job);
        long result =
                sql.queryForObject("SELECT id FROM ocr_result WHERE job_id=?", Long.class, job);
        sql.update(
                "INSERT INTO"
                    + " field_confirmation(document_id,ocr_result_id,field_code,confirmed_value,confirmed_by)"
                    + " VALUES(?,?,'businessName','가상상호',?)",
                doc,
                result,
                actor.id());
        sql.update(
                "INSERT INTO supplement_request(application_id,requested_by,reason,due_at)"
                        + " VALUES(?,?,'가상 보완',UTC_TIMESTAMP())",
                app,
                actor.id());
        long request =
                sql.queryForObject(
                        "SELECT id FROM supplement_request WHERE application_id=?",
                        Long.class,
                        app);
        sql.update(
                "INSERT INTO supplement_item(request_id,target_code,instruction)"
                        + " VALUES(?,'BUSINESS_CERT','확인')",
                request);
        sql.update(
                "INSERT INTO supplement_response(request_id,revision_id,response_text,responded_by)"
                        + " VALUES(?,?,'보완',?)",
                request,
                revision,
                actor.id());
        sql.update(
                "INSERT INTO review(application_id,reviewer_id,check_items) VALUES(?,?,'{}')",
                app,
                actor.id());
        sql.update(
                "INSERT INTO staff_assignment(application_id,staff_id,assigned_by,reason)"
                        + " VALUES(?,?,?,'검증')",
                app,
                actor.id(),
                actor.id());
        assertEquals(1, maintenance.resetBefore(login(actor), LocalDateTime.now().plusDays(1)));
        assertEquals(
                0,
                sql.queryForObject(
                        "SELECT COUNT(*) FROM application WHERE id=?", Integer.class, app));
        assertEquals(
                0,
                sql.queryForObject(
                        "SELECT COUNT(*) FROM ocr_result WHERE id=?", Integer.class, result));
        assertEquals(
                0,
                sql.queryForObject(
                        "SELECT COUNT(*) FROM audit_event WHERE application_id=?",
                        Integer.class,
                        app));
        assertEquals(
                1,
                sql.queryForObject(
                        "SELECT COUNT(*) FROM app_user WHERE id=?", Integer.class, actor.id()));
        assertTrue(programs.count() > 0);
    }
}
