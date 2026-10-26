package kr.co.maeum.support;

import static org.junit.jupiter.api.Assertions.*;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.maeum.support.application.ApplicationMapper;
import kr.co.maeum.support.application.ApplicationService;
import kr.co.maeum.support.application.SampleApplicationService;
import kr.co.maeum.support.application.SampleDatasetPolicy;
import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.config.WebConfig;
import kr.co.maeum.support.document.DocumentMapper;
import kr.co.maeum.support.identity.IdentityMapper;
import kr.co.maeum.support.identity.PortalPrincipal;
import kr.co.maeum.support.identity.RegistrationService;
import kr.co.maeum.support.program.ProgramMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

@SpringJUnitConfig(WebConfig.class)
@WebAppConfiguration
@Transactional
class ProgramSampleIntegrationTest {
    @Autowired RegistrationService registration;
    @Autowired IdentityMapper identities;
    @Autowired ProgramMapper programs;
    @Autowired ApplicationService workflow;
    @Autowired ApplicationMapper applications;
    @Autowired SampleApplicationService samples;
    @Autowired DocumentMapper documents;

    private PortalPrincipal applicant() {
        String login = "sample_" + UUID.randomUUID().toString().replace("-", "").substring(0, 16);
        registration.register(
                login, "샘플 확인 신청자", "Only-test-password-2026!", "Only-test-password-2026!");
        return identities.findLogin(login).principal();
    }

    @Test
    @DisplayName("새 사업 다섯 개는 대응하는 사례만 불러오며 신청 정보·사업비·파일 정답이 일치한다")
    void five_program_samples_match_their_program_and_keep_documents_explicit() throws Exception {
        var actor = applicant();
        var mapper = new ObjectMapper();
        for (String dataset : List.of("digital", "energy", "access", "online", "safety")) {
            try (var stream =
                    getClass().getResourceAsStream("/fixtures/" + dataset + "/application.json")) {
                assertNotNull(stream);
                var data = mapper.readTree(stream);
                var program = programs.findBySeedKey(data.path("programSeedKey").asText());
                assertEquals(dataset, SampleDatasetPolicy.choices(program).getFirst().code());
                long id = workflow.start(actor, program.getId());
                samples.apply(actor, id, applications.find(id).getVersion(), dataset);
                var application = applications.find(id);
                var expected = data.path("application");
                assertEquals(expected.path("businessName").asText(), application.getBusinessName());
                assertEquals(expected.path("planText").asText(), application.getPlanText());
                assertEquals(
                        0,
                        application
                                .getRequestedAmount()
                                .compareTo(expected.path("requestedAmount").decimalValue()));
                assertEquals(
                        0,
                        application
                                .getSelfContribution()
                                .compareTo(expected.path("selfContribution").decimalValue()));
                assertEquals(
                        0,
                        application
                                .getRequestedAmount()
                                .add(application.getSelfContribution())
                                .compareTo(
                                        data.path("estimate").path("totalAmount").decimalValue()));
                assertTrue(documents.current(id).isEmpty());
                for (String kind : List.of("business-cert", "estimate")) {
                    try (var image =
                            getClass()
                                    .getResourceAsStream(
                                            "/fixtures/" + dataset + "/" + kind + ".png")) {
                        assertNotNull(image);
                        assertArrayEquals(new byte[] {(byte) 137, 80, 78, 71}, image.readNBytes(4));
                    }
                }
            }
        }
    }

    @Test
    @DisplayName("다른 공고용 샘플과 다른 신청자 접근은 거부하며 기존 신청을 바꾸지 않는다")
    void mismatched_dataset_and_other_owner_are_rejected_without_changes() {
        var actor = applicant();
        var program = programs.findBySeedKey("local-digital-store-v1");
        long id = workflow.start(actor, program.getId());
        long version = applications.find(id).getVersion();
        assertEquals(
                422,
                assertThrows(
                                BusinessException.class,
                                () -> samples.apply(actor, id, version, "energy"))
                        .getStatus());
        var other = applicant();
        assertEquals(
                403,
                assertThrows(
                                BusinessException.class,
                                () -> samples.apply(other, id, version, "digital"))
                        .getStatus());
        assertNull(applications.find(id).getBusinessName());
        assertEquals(version, applications.find(id).getVersion());
    }
}
