package kr.co.maeum.support.mapper;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.Map;

@EgovMapper
public interface DemoMapper {
    Long scenario(String code);

    void insertWorkspace(Map<String, Object> workspace);

    void expire(long workspaceId);
}
