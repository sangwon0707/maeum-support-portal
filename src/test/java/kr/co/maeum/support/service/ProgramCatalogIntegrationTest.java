package kr.co.maeum.support.service;

import static org.junit.jupiter.api.Assertions.*;

import kr.co.maeum.support.config.LocalSeed;
import kr.co.maeum.support.config.WebConfig;
import kr.co.maeum.support.mapper.ProgramMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.junit.jupiter.SpringJUnitConfig;
import org.springframework.test.context.web.WebAppConfiguration;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@SpringJUnitConfig(WebConfig.class)
@WebAppConfiguration
@Transactional
class ProgramCatalogIntegrationTest {
    @Autowired LocalSeed seed;
    @Autowired ProgramMapper programs;

    private final List<String> seedKeys =
            List.of(
                    "local-store-safety-v1",
                    "local-online-channel-v1",
                    "local-accessible-store-v1",
                    "local-energy-saving-v1",
                    "local-digital-store-v1");

    @Test
    @DisplayName("가상 사업은 다시 실행해도 중복되지 않으며 사업별 내용과 필수 서류를 보존한다")
    void catalog_is_distinct_and_repeated_seed_does_not_duplicate() {
        long before = programs.count();
        seed.seed();
        seed.seed();
        assertEquals(before, programs.count());
        var catalog = seedKeys.stream().map(programs::findBySeedKey).toList();
        assertTrue(catalog.stream().allMatch(program -> program != null));
        assertEquals(5, catalog.stream().map(program -> program.getTitle()).distinct().count());
        assertEquals(
                5, catalog.stream().map(program -> program.getSupportText()).distinct().count());
        assertEquals(
                5, catalog.stream().map(program -> program.getSelectionText()).distinct().count());
        for (var program : catalog) {
            assertTrue(program.getBody().contains("가상"));
            assertTrue(program.getBody().contains("실제 지원금"));
            assertEquals(
                    List.of("BUSINESS_CERT", "ESTIMATE"), programs.requiredTypes(program.getId()));
            assertTrue(program.getOpensAt().isBefore(program.getClosesAt()));
        }
    }

    @Test
    @DisplayName("관리자가 가상 사업의 제목과 내용을 수정해도 초기화 시 덮어쓰거나 복제하지 않는다")
    void edited_catalog_program_is_preserved_on_seed() {
        var program = programs.findBySeedKey("local-digital-store-v1");
        long before = programs.count();
        program.setTitle("관리자가 수정한 디지털 사업 제목");
        program.setSupportText("관리자가 수정한 지원 항목");
        assertEquals(1, programs.update(program));
        seed.seed();
        assertEquals(before, programs.count());
        var after = programs.findBySeedKey("local-digital-store-v1");
        assertEquals(program.getId(), after.getId());
        assertEquals(program.getTitle(), after.getTitle());
        assertEquals(program.getSupportText(), after.getSupportText());
    }
}
