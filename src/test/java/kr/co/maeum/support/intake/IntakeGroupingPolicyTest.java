package kr.co.maeum.support.intake;

import static org.junit.jupiter.api.Assertions.*;

import kr.co.maeum.support.common.BusinessException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

class IntakeGroupingPolicyTest {
    private final IntakeGroupingPolicy policy = new IntakeGroupingPolicy();

    private List<Map<String, Object>> pages(long... sources) {
        var pages = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < sources.length; index++) {
            var page = new LinkedHashMap<String, Object>();
            page.put("sourceId", sources[index]);
            page.put("sourceName", "신청.pdf");
            page.put("pageNumber", index + 1);
            pages.add(page);
        }
        return pages;
    }

    @Test
    @DisplayName("연속된 계획서 두 페이지는 한 문서로 유지한다")
    void consecutive_plan_pages_remain_one_document() {
        var groups =
                policy.group(
                        pages(1, 1, 1),
                        List.of("APPLICATION", "BUSINESS_PLAN", "BUSINESS_PLAN"),
                        List.of());
        assertEquals(2, groups.size());
        assertEquals(List.of(2, 3), groups.get(1).get("pageNumbers"));
    }

    @Test
    @DisplayName("사용자가 지정한 경계와 원본 파일 경계는 문서를 나눈다")
    void explicit_boundary_and_source_boundary_split_documents() {
        assertEquals(
                2,
                policy.group(pages(1, 1), List.of("BUSINESS_PLAN", "BUSINESS_PLAN"), List.of(1))
                        .size());
        assertEquals(
                2,
                policy.group(pages(1, 2), List.of("BUSINESS_PLAN", "BUSINESS_PLAN"), List.of())
                        .size());
    }

    @Test
    @DisplayName("페이지 수와 종류가 다르거나 자동 분류 상태인 반영을 거부한다")
    void invalid_page_types_cannot_be_applied() {
        assertThrows(
                BusinessException.class,
                () -> policy.group(pages(1, 1), List.of("APPLICATION"), List.of()));
        assertThrows(
                BusinessException.class, () -> policy.group(pages(1), List.of("AUTO"), List.of()));
    }
}
