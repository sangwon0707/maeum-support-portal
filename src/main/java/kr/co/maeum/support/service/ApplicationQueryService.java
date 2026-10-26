package kr.co.maeum.support.service;

import kr.co.maeum.support.mapper.ApplicationMapper;
import kr.co.maeum.support.model.ApplicationStatus;
import kr.co.maeum.support.security.AccessScope;
import kr.co.maeum.support.security.PortalPrincipal;

import org.egovframe.rte.ptl.mvc.tags.ui.pagination.PaginationInfo;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.Map;

@Service
public class ApplicationQueryService {
    private final ApplicationMapper mapper;
    private final AccessScope access;

    public ApplicationQueryService(ApplicationMapper mapper, AccessScope access) {
        this.mapper = mapper;
        this.access = access;
    }

    public Map<String, Object> page(
            PortalPrincipal actor,
            int page,
            String keyword,
            String status,
            LocalDate from,
            LocalDate until) {
        access.active(actor);
        var filter = scope(actor);
        filter.put("keyword", keyword);
        filter.put("status", status);
        filter.put("from", from == null ? null : from.atStartOfDay().minusHours(9));
        filter.put("until", until == null ? null : until.plusDays(1).atStartOfDay().minusHours(9));
        PaginationInfo pagination = new PaginationInfo();
        pagination.setRecordCountPerPage(20);
        pagination.setPageSize(10);
        pagination.setTotalRecordCount((int) Math.min(Integer.MAX_VALUE, mapper.count(filter)));
        pagination.setCurrentPageNo(
                Math.min(Math.max(1, page), Math.max(1, pagination.getTotalPageCount())));
        filter.put("limit", 20);
        filter.put("offset", pagination.getFirstRecordIndex());
        return Map.of("applications", mapper.list(filter), "pagination", pagination);
    }

    public Map<String, Long> mySummary(PortalPrincipal actor) {
        access.role(actor, "APPLICANT");
        var counts = new java.util.LinkedHashMap<String, Long>();
        for (var status : ApplicationStatus.values()) {
            counts.put(status.name(), 0L);
        }
        long total = 0;
        for (var row : mapper.statusCounts(scope(actor))) {
            long count = ((Number) row.get("total")).longValue();
            counts.put(row.get("status").toString(), count);
            total += count;
        }
        counts.put("total", total);
        return java.util.Collections.unmodifiableMap(counts);
    }

    public Map<String, Object> scope(PortalPrincipal actor) {
        access.active(actor);
        var scope = new HashMap<String, Object>();
        scope.put("workspaceId", actor.workspaceId());
        if ("APPLICANT".equals(actor.role())) {
            scope.put("ownerId", actor.id());
        }
        if ("STAFF".equals(actor.role())) {
            scope.put("staffId", actor.id());
        }
        return scope;
    }
}
