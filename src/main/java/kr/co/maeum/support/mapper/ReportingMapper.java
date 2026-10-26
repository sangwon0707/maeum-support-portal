package kr.co.maeum.support.mapper;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;
import java.util.Map;

@EgovMapper
public interface ReportingMapper {
    List<Map<String, Object>> statusCounts(Map<String, Object> scope);

    List<Map<String, Object>> staffCounts(Map<String, Object> scope);

    List<Map<String, Object>> overdue(Map<String, Object> scope);
}
