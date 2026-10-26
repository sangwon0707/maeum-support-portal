package kr.co.maeum.support.audit;

import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;
import java.util.Map;

@EgovMapper
public interface AuditMapper {
    void insert(Map<String, Object> event);

    List<Map<String, Object>> list(
            @Param("workspaceId") long workspaceId, @Param("keyword") String keyword);
}
