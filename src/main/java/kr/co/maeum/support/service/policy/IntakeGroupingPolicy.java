package kr.co.maeum.support.service.policy;

import kr.co.maeum.support.common.BusinessException;

import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/** 페이지 수와 문서 수를 구분하고, 원본 파일 안의 연속된 페이지만 같은 문서로 묶는다. */
@Component
public class IntakeGroupingPolicy {
    public List<Map<String, Object>> group(
            List<Map<String, Object>> pages, List<String> kinds, List<Integer> starts) {
        if (pages.size() != kinds.size()
                || kinds.stream()
                        .anyMatch(
                                kind -> !IntakePolicy.KINDS.contains(kind) || "AUTO".equals(kind)))
            throw new BusinessException(422, "페이지별 문서 종류를 확인해 주세요.");
        var groups = new ArrayList<Map<String, Object>>();
        for (int index = 0; index < pages.size(); index++) {
            var page = pages.get(index);
            String kind = kinds.get(index);
            var previous = groups.isEmpty() ? null : groups.getLast();
            boolean begin =
                    previous == null
                            || starts.contains(index)
                            || !kind.equals(previous.get("kind"))
                            || !page.get("sourceId")
                                    .toString()
                                    .equals(previous.get("sourceId").toString());
            if (begin) {
                previous = new LinkedHashMap<>();
                previous.put("sourceId", page.get("sourceId"));
                previous.put("sourceName", page.get("sourceName"));
                previous.put("kind", kind);
                previous.put("kindLabel", IntakePolicy.label(kind));
                previous.put("pageNumbers", new ArrayList<Integer>());
                previous.put("indexes", new ArrayList<Integer>());
                groups.add(previous);
            }
            @SuppressWarnings("unchecked")
            var numbers = (List<Integer>) previous.get("pageNumbers");
            @SuppressWarnings("unchecked")
            var indexes = (List<Integer>) previous.get("indexes");
            numbers.add(((Number) page.get("pageNumber")).intValue());
            indexes.add(index);
            page.put("kind", kind);
            page.put("kindLabel", IntakePolicy.label(kind));
            page.put("groupStart", begin);
        }
        return groups;
    }
}
