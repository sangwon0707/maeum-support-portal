package kr.co.maeum.support.mapper;

import kr.co.maeum.support.model.Program;

import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;

@EgovMapper
public interface ProgramMapper {
    Program find(long id);

    Program findBySeedKey(String seedKey);

    int bindSeedKey(@Param("id") long id, @Param("seedKey") String seedKey);

    List<Program> list(
            @Param("keyword") String keyword,
            @Param("state") String state,
            @Param("admin") boolean admin);

    void insert(Program program);

    int update(Program program);

    List<String> requiredTypes(long programId);

    void insertRule(@Param("programId") long programId, @Param("type") String type);

    long count();
}
