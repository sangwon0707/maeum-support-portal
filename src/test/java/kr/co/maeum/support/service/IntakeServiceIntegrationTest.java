package kr.co.maeum.support.service;

import static org.junit.jupiter.api.Assertions.*;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.common.JsonCodec;
import kr.co.maeum.support.config.WebConfig;
import kr.co.maeum.support.dto.ApplicationForm;
import kr.co.maeum.support.mapper.ApplicationMapper;
import kr.co.maeum.support.mapper.IdentityMapper;
import kr.co.maeum.support.mapper.IntakeMapper;
import kr.co.maeum.support.mapper.ProgramMapper;
import kr.co.maeum.support.security.PortalPrincipal;
import kr.co.maeum.support.storage.IntakeStorage;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@SpringJUnitConfig(WebConfig.class)
@WebAppConfiguration
class IntakeServiceIntegrationTest {
    @Autowired IntakeService intake;
    @Autowired IntakeMapper jobs;
    @Autowired IntakeStorage storage;
    @Autowired ApplicationService workflow;
    @Autowired ApplicationMapper applications;
    @Autowired SampleApplicationService samples;
    @Autowired ProgramMapper programs;
    @Autowired RegistrationService registration;
    @Autowired IdentityMapper identities;
    @Autowired JsonCodec json;
    @Autowired PlatformTransactionManager manager;

    private PortalPrincipal member() {
        String login = "intake_" + UUID.randomUUID().toString().replace("-", "").substring(0, 20);
        registration.register(
                login, "가상 신청자", "Only-test-password-2026!", "Only-test-password-2026!");
        return identities.findLogin(login).principal();
    }

    private record Fixture(
            PortalPrincipal owner, long applicationId, long intakeId, String previewKey) {}

    private Fixture ready() throws Exception {
        var owner = member();
        long applicationId =
                workflow.start(owner, programs.findBySeedKey("local-store-environment-v1").getId());
        samples.apply(
                owner, applicationId, applications.find(applicationId).getVersion(), "original");
        byte[] pdf;
        byte[] png;
        try (var stream = getClass().getResourceAsStream("/fixtures/original/application.pdf")) {
            pdf = stream.readAllBytes();
        }
        try (var stream = getClass().getResourceAsStream("/fixtures/original/business-cert.png")) {
            png = stream.readAllBytes();
        }
        String sourceKey = storage.save(pdf, true), previewKey = storage.save(png, false);
        var job = new HashMap<String, Object>();
        job.put("applicationId", applicationId);
        job.put("createdBy", owner.id());
        job.put("layout", "BUNDLE");
        // 공급자 호출과 별개로 상태·권한·트랜잭션 경계를 검증하는 READY 테스트 픽스처다.
        new TransactionTemplate(manager)
                .executeWithoutResult(
                        status -> {
                            jobs.insert(job);
                            long id = ((Number) job.get("id")).longValue();
                            var source =
                                    Map.<String, Object>of(
                                            "intakeId",
                                            id,
                                            "kind",
                                            "AUTO",
                                            "originalName",
                                            "검증.pdf",
                                            "storageKey",
                                            sourceKey,
                                            "mimeType",
                                            "application/pdf",
                                            "sizeBytes",
                                            pdf.length);
                            jobs.source(source);
                            jobs.enqueue(id);
                            jobs.claim(id, "test-claim");
                            Object sourceId = jobs.sources(id).getFirst().get("id");
                            var page =
                                    Map.of(
                                            "sourceId",
                                            sourceId,
                                            "sourceName",
                                            "검증.pdf",
                                            "pageNumber",
                                            1,
                                            "kind",
                                            "APPLICATION",
                                            "previewKey",
                                            previewKey,
                                            "photos",
                                            List.of());
                            var group =
                                    Map.of(
                                            "sourceId",
                                            sourceId,
                                            "sourceName",
                                            "검증.pdf",
                                            "pageNumbers",
                                            List.of(1),
                                            "kind",
                                            "APPLICATION",
                                            "pdfKey",
                                            sourceKey);
                            jobs.finish(
                                    id,
                                    "test-claim",
                                    json.write(
                                            Map.of(
                                                    "pages",
                                                    List.of(page),
                                                    "groups",
                                                    List.of(group),
                                                    "fields",
                                                    Map.of())));
                        });
        return new Fixture(owner, applicationId, ((Number) job.get("id")).longValue(), previewKey);
    }

    @Test
    @DisplayName("타인 신청자와 등록되지 않은 파일 키의 접근을 거부한다")
    void other_owner_and_unregistered_assets_are_forbidden() throws Exception {
        var fixture = ready();
        var other = member();
        assertThrows(
                BusinessException.class,
                () -> intake.view(other, fixture.applicationId(), fixture.intakeId()));
        assertThrows(
                BusinessException.class,
                () -> intake.asset(other, fixture.intakeId(), fixture.previewKey()));
        assertThrows(
                BusinessException.class,
                () ->
                        intake.asset(
                                fixture.owner(), fixture.intakeId(), UUID.randomUUID() + ".png"));
        assertTrue(
                java.nio.file.Files.exists(
                        intake.asset(fixture.owner(), fixture.intakeId(), fixture.previewKey())));
    }

    @Test
    @DisplayName("버전 충돌 시 신청값과 분석 상태를 함께 롤백하고 중복 반영은 재저장하지 않는다")
    void stale_version_rolls_back_and_repeat_apply_is_idempotent() throws Exception {
        var fixture = ready();
        var before = applications.find(fixture.applicationId());
        var form = new ApplicationForm();
        BeanUtils.copyProperties(before, form);
        form.setVersion(before.getVersion() - 1);
        form.setBusinessName("덮어쓰면 안 되는 값");
        assertThrows(
                BusinessException.class,
                () ->
                        intake.apply(
                                fixture.owner(),
                                fixture.applicationId(),
                                fixture.intakeId(),
                                form,
                                List.of("APPLICATION"),
                                List.of(),
                                List.of()));
        assertEquals(
                before.getBusinessName(),
                applications.find(fixture.applicationId()).getBusinessName());
        assertEquals("READY", jobs.find(fixture.intakeId()).get("status"));
        form.setVersion(before.getVersion());
        form.setBusinessName("검토 후 확정 상호");
        assertFalse(
                intake.apply(
                        fixture.owner(),
                        fixture.applicationId(),
                        fixture.intakeId(),
                        form,
                        List.of("APPLICATION"),
                        List.of(),
                        List.of()));
        assertEquals("APPLIED", jobs.find(fixture.intakeId()).get("status"));
        long version = applications.find(fixture.applicationId()).getVersion();
        intake.apply(
                fixture.owner(),
                fixture.applicationId(),
                fixture.intakeId(),
                form,
                List.of("APPLICATION"),
                List.of(),
                List.of());
        assertEquals(version, applications.find(fixture.applicationId()).getVersion());
    }

    @Test
    @DisplayName("반영 후 수정은 소유자의 작성 중 신청에만 저장하고 이전 버전 저장을 거부한다")
    void reviewed_content_can_be_revised_only_by_owner_with_current_version() throws Exception {
        var fixture = ready();
        var form = new ApplicationForm();
        BeanUtils.copyProperties(applications.find(fixture.applicationId()), form);
        assertThrows(
                BusinessException.class,
                () ->
                        intake.saveReviewedContent(
                                fixture.owner(),
                                fixture.applicationId(),
                                fixture.intakeId(),
                                form));
        intake.apply(
                fixture.owner(),
                fixture.applicationId(),
                fixture.intakeId(),
                form,
                List.of("APPLICATION"),
                List.of(),
                List.of());
        BeanUtils.copyProperties(applications.find(fixture.applicationId()), form);
        form.setPhone("000-0000-0999");
        var other = member();
        assertThrows(
                BusinessException.class,
                () ->
                        intake.saveReviewedContent(
                                other, fixture.applicationId(), fixture.intakeId(), form));
        intake.saveReviewedContent(
                fixture.owner(), fixture.applicationId(), fixture.intakeId(), form);
        assertEquals("000-0000-0999", applications.find(fixture.applicationId()).getPhone());
        assertThrows(
                BusinessException.class,
                () ->
                        intake.saveReviewedContent(
                                fixture.owner(),
                                fixture.applicationId(),
                                fixture.intakeId(),
                                form));
    }

    @Test
    @DisplayName("업로드·조회만으로 분석하지 않고 명시적 시작만 한 번 큐에 넣는다")
    void upload_does_not_schedule_ocr_until_explicit_start() throws Exception {
        byte[] content;
        try (var stream = getClass().getResourceAsStream("/fixtures/digital/application.pdf")) {
            content = stream.readAllBytes();
        }
        String previous = System.getProperty("OCR_LIVE_ENABLED");
        System.setProperty("OCR_LIVE_ENABLED", "true");
        try {
            new TransactionTemplate(manager)
                    .executeWithoutResult(
                            status -> {
                                try {
                                    var owner = member();
                                    long programId =
                                            programs.findBySeedKey("local-store-environment-v1")
                                                    .getId();
                                    long applicationId = workflow.start(owner, programId);
                                    String key = UUID.randomUUID().toString();
                                    var uploads =
                                            List.of(
                                                    new IntakeService.Upload(
                                                            "신청.pdf", "AUTO", content));
                                    long first =
                                            intake.upload(
                                                    owner, applicationId, "BUNDLE", uploads, key);
                                    assertEquals("UPLOADED", jobs.find(first).get("status"));
                                    assertFalse(jobs.pending().contains(first));
                                    assertEquals(0, jobs.claim(first, "must-not-claim"));
                                    intake.view(owner, applicationId, first);
                                    intake.view(owner, applicationId, first);
                                    assertEquals("UPLOADED", jobs.find(first).get("status"));
                                    assertEquals(
                                            first,
                                            intake.upload(
                                                    owner, applicationId, "BUNDLE", uploads, key));
                                    assertEquals(
                                            false,
                                            intake.activeForProgram(owner, programId)
                                                    .get("active"));
                                    var other = member();
                                    assertThrows(
                                            BusinessException.class,
                                            () ->
                                                    intake.startAnalysis(
                                                            other, applicationId, first));
                                    intake.startAnalysis(owner, applicationId, first);
                                    assertEquals("QUEUED", jobs.find(first).get("status"));
                                    intake.startAnalysis(owner, applicationId, first);
                                    assertEquals("QUEUED", jobs.find(first).get("status"));
                                    assertEquals(
                                            true,
                                            intake.activeForProgram(owner, programId)
                                                    .get("active"));
                                    long second =
                                            intake.upload(
                                                    owner,
                                                    applicationId,
                                                    "BUNDLE",
                                                    uploads,
                                                    UUID.randomUUID().toString());
                                    assertEquals("UPLOADED", jobs.find(second).get("status"));
                                    assertThrows(
                                            BusinessException.class,
                                            () ->
                                                    intake.startAnalysis(
                                                            owner, applicationId, second));
                                    var changed =
                                            List.of(
                                                    new IntakeService.Upload(
                                                            "다른.pdf", "APPLICATION", content));
                                    assertThrows(
                                            BusinessException.class,
                                            () ->
                                                    intake.upload(
                                                            owner,
                                                            applicationId,
                                                            "BUNDLE",
                                                            changed,
                                                            key));
                                    jobs.claim(first, "test-claim");
                                    jobs.finish(first, "test-claim", "{}");
                                    assertEquals(
                                            first,
                                            intake.upload(
                                                    owner, applicationId, "BUNDLE", uploads, key));
                                    assertEquals(
                                            false,
                                            intake.activeForProgram(owner, programId)
                                                    .get("active"));
                                } finally {
                                    status.setRollbackOnly();
                                }
                            });
        } finally {
            if (previous == null) {
                System.clearProperty("OCR_LIVE_ENABLED");
            } else {
                System.setProperty("OCR_LIVE_ENABLED", previous);
            }
        }
    }
}
