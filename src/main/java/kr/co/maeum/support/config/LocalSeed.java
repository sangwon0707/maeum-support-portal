package kr.co.maeum.support.config;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.maeum.support.demo.DemoService;
import kr.co.maeum.support.identity.IdentityMapper;
import kr.co.maeum.support.program.Program;
import kr.co.maeum.support.program.ProgramMapper;

import org.springframework.context.event.ContextRefreshedEvent;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

/** 개발용 계정과 가상 공고만 준비한다. 운영 환경에서는 LOCAL_SEED=false로 비활성화한다. */
@Component
public class LocalSeed {
    private final ProgramMapper programs;
    private final IdentityMapper identities;
    private final DemoService demos;
    private final Clock clock;

    public LocalSeed(
            ProgramMapper programs, IdentityMapper identities, DemoService demos, Clock clock) {
        this.programs = programs;
        this.identities = identities;
        this.demos = demos;
        this.clock = clock;
    }

    @EventListener(ContextRefreshedEvent.class)
    @Transactional
    public void seed() {
        if (!Boolean.parseBoolean(DatabaseConfig.env("LOCAL_SEED", "true"))) {
            return;
        }
        if (programs.count() == 0) {
            Program program = new Program();
            program.setSeedKey("local-store-environment-v1");
            program.setTitle("2026 소상공인 매장 환경개선 지원사업");
            program.setBody(
                    "간판과 조명 등 매장 환경을 개선하려는 소상공인의 신청을 받습니다. 제출 서류를 담당자가 확인하고 보완·선정 결과를 안내합니다. 이 공고는"
                            + " 포트폴리오용 가상 사업이며 실제 지원금은 지급하지 않습니다.");
            program.setTargetText("영업 중인 소상공인으로 매장 환경개선 계획을 가진 사업자");
            program.setSupportText("간판 교체, 조명 개선, 전기 배선 정비 등 매장 환경개선 비용. 지원금 신청액과 자부담을 구분해 작성합니다.");
            program.setExclusionText("이미 완료한 공사, 사업과 무관한 지출, 동일 비용의 중복 지원은 검토 대상에서 제외합니다.");
            program.setSelectionText("필수 서류, 신청 정보와 증빙의 일치, 계획의 구체성 및 견적 적정성을 검토합니다.");
            program.setContactText("마음이음 사업운영팀 · 실제 운영기관 설정 후 문의처 안내");
            program.setStatus("PUBLISHED");
            program.setOpensAt(LocalDateTime.now(clock).minusDays(7));
            program.setClosesAt(LocalDateTime.now(clock).plusDays(30));
            program.setMaxAmount(new BigDecimal("10000000"));
            programs.insert(program);
            programs.insertRule(program.getId(), "BUSINESS_CERT");
            programs.insertRule(program.getId(), "ESTIMATE");
        }
        // 이전 버전의 기본 공고는 내용·금액을 바꾸지 않고 예시 연결 식별자만 부여한다.
        if (programs.findBySeedKey("local-store-environment-v1") == null) {
            programs.list("2026 소상공인 매장 환경개선 지원사업", "", false).stream()
                    .filter(program -> "2026 소상공인 매장 환경개선 지원사업".equals(program.getTitle()))
                    .filter(program -> program.getSeedKey() == null)
                    .findFirst()
                    .ifPresent(
                            program ->
                                    programs.bindSeedKey(
                                            program.getId(), "local-store-environment-v1"));
        }
        seedAdditionalPrograms();
        String password = DatabaseConfig.env("LOCAL_ACCOUNT_PASSWORD", "Portfolio2026!");
        for (String role : List.of("APPLICANT", "STAFF", "ADMIN")) {
            String login = role.toLowerCase();
            if (identities.findLogin(login) == null) {
                demos.createUser(
                        1,
                        login,
                        switch (role) {
                            case "APPLICANT" -> "개발용 신청자";
                            case "STAFF" -> "개발용 담당자";
                            default -> "개발용 관리자";
                        },
                        role,
                        password);
            }
        }
    }

    private void seedAdditionalPrograms() {
        LocalDateTime now = LocalDateTime.now(clock).withSecond(0).withNano(0);
        try (var stream =
                getClass().getResourceAsStream("/fixtures/programs/additional-programs.json")) {
            if (stream == null) {
                throw new IllegalStateException("가상 사업 목록을 찾을 수 없습니다.");
            }
            List<SeedProgram> catalog =
                    new ObjectMapper().readValue(stream, new TypeReference<>() {});
            for (var entry : catalog) {
                // 고정 식별자만 확인하며, 관리자가 바꾼 공고 내용과 접수 기간은 보존한다.
                if (programs.findBySeedKey(entry.seedKey()) != null) {
                    continue;
                }
                Program program = new Program();
                program.setSeedKey(entry.seedKey());
                program.setTitle(entry.title());
                program.setBody(entry.body());
                program.setTargetText(entry.targetText());
                program.setSupportText(entry.supportText());
                program.setExclusionText(entry.exclusionText());
                program.setSelectionText(entry.selectionText());
                program.setContactText(entry.contactText());
                program.setMaxAmount(entry.maxAmount());
                program.setStatus("PUBLISHED");
                program.setOpensAt(now.plusDays(entry.opensOffsetDays()));
                program.setClosesAt(now.plusDays(entry.closesOffsetDays()));
                programs.insert(program);
                programs.insertRule(program.getId(), "BUSINESS_CERT");
                programs.insertRule(program.getId(), "ESTIMATE");
            }
        } catch (IOException exception) {
            throw new IllegalStateException("가상 사업 목록을 읽을 수 없습니다.", exception);
        }
    }

    private record SeedProgram(
            String seedKey,
            String title,
            String body,
            String targetText,
            String supportText,
            String exclusionText,
            String selectionText,
            String contactText,
            BigDecimal maxAmount,
            int opensOffsetDays,
            int closesOffsetDays) {}
}
