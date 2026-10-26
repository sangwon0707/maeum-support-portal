package kr.co.maeum.support.mapper;

import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;
import java.util.Map;

@EgovMapper
public interface IntakeMapper {
    void insert(Map<String, Object> intake);

    void source(Map<String, Object> source);

    Map<String, Object> find(long id);

    Map<String, Object> lock(long id);

    Map<String, Object> latest(long applicationId);

    Map<String, Object> byRequest(
            @Param("applicationId") long applicationId, @Param("requestKey") String requestKey);

    Map<String, Object> activeForProgram(
            @Param("workspaceId") long workspaceId,
            @Param("ownerId") long ownerId,
            @Param("programId") long programId);

    List<Map<String, Object>> sources(long intakeId);

    List<Long> pending();

    int enqueue(long id);

    int beginPrepared(long id);

    java.util.List<java.util.Map<String, Object>> replaying();

    int replayProgress(
            @Param("id") long id,
            @Param("stage") String stage,
            @Param("progress") int progress,
            @Param("detail") String detail);

    int claim(@Param("id") long id, @Param("token") String token);

    int progress(
            @Param("id") long id,
            @Param("token") String token,
            @Param("stage") String stage,
            @Param("progress") int progress,
            @Param("detail") String detail);

    int finish(@Param("id") long id, @Param("token") String token, @Param("result") String result);

    int fail(@Param("id") long id, @Param("token") String token, @Param("error") String error);

    int applied(@Param("id") long id, @Param("result") String result);

    void expireInterrupted();

    Map<String, Object> cacheScope(long id);

    String cached(@Param("scope") Map<String, Object> scope, @Param("version") String version);

    void cache(
            @Param("scope") Map<String, Object> scope,
            @Param("version") String version,
            @Param("payload") String payload);
}
