package kr.co.maeum.support.identity;

import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;
import java.util.Map;

@EgovMapper
public interface IdentityMapper {
    Account findLogin(String loginId);

    List<Account> listStaff(long workspaceId);

    Account findRole(@Param("workspaceId") long workspaceId, @Param("role") String role);

    int activeWorkspace(long workspaceId);

    void insertAccount(Map<String, Object> account);

    void insertRole(@Param("userId") long userId, @Param("role") String role);
}
