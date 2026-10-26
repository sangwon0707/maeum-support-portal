package kr.co.maeum.support.service;

import static org.junit.jupiter.api.Assertions.*;

import kr.co.maeum.support.config.WebConfig;
import kr.co.maeum.support.mapper.IdentityMapper;
import kr.co.maeum.support.mapper.ProgramMapper;
import kr.co.maeum.support.security.PortalPrincipal;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;

import java.util.UUID;

@SpringJUnitConfig(WebConfig.class)
@WebAppConfiguration
class MyPageIntegrationTest {
    @Autowired RegistrationService registration;
    @Autowired IdentityMapper identities;
    @Autowired ApplicationService applications;
    @Autowired ApplicationQueryService queries;
    @Autowired ProgramMapper programs;

    private PortalPrincipal member() {
        String username =
                "profile_" + UUID.randomUUID().toString().replace("-", "").substring(0, 18);
        registration.register(
                username, "가상 신청자", "Only-test-password-2026!", "Only-test-password-2026!");
        return identities.findLogin(username).principal();
    }

    @Test
    @DisplayName("마이페이지 건수와 상태별 목록은 같은 공간의 다른 신청자를 포함하지 않는다")
    void summary_and_filtered_list_are_scoped_to_owner() {
        var owner = member();
        var other = member();
        long first = programs.findBySeedKey("local-store-environment-v1").getId();
        long second =
                programs.list("", "", false).stream()
                        .filter(program -> program.getId() != first)
                        .findFirst()
                        .orElseThrow()
                        .getId();
        applications.start(owner, first);
        applications.start(other, first);
        applications.start(other, second);
        var summary = queries.mySummary(owner);
        assertEquals(1L, summary.get("total"));
        assertEquals(1L, summary.get("DRAFT"));
        assertEquals(0L, summary.get("SUBMITTED"));
        assertEquals(0L, summary.get("NEEDS_SUPPLEMENT"));
        assertEquals(2L, queries.mySummary(other).get("total"));
        var drafts =
                (java.util.List<?>)
                        queries.page(owner, 1, "", "DRAFT", null, null).get("applications");
        var submitted =
                (java.util.List<?>)
                        queries.page(owner, 1, "", "SUBMITTED", null, null).get("applications");
        assertEquals(1, drafts.size());
        assertTrue(submitted.isEmpty());
    }
}
