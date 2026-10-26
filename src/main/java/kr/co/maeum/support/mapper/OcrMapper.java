package kr.co.maeum.support.mapper;

import kr.co.maeum.support.model.OcrJob;

import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;
import java.util.Map;

@EgovMapper
public interface OcrMapper {
    OcrJob findDocumentJob(long documentId);

    void insert(
            @Param("documentId") long documentId,
            @Param("mode") String mode,
            @Param("version") String version);

    OcrJob findModeJob(@Param("documentId") long documentId, @Param("mode") String mode);

    long countWorkspace(long workspaceId);

    long countOwnerToday(@Param("workspaceId") long workspaceId, @Param("ownerId") long ownerId);

    List<OcrJob> pending();

    void expireExhausted();

    int claim(@Param("id") long id, @Param("token") String token);

    void result(
            @Param("jobId") long jobId,
            @Param("fields") String fields,
            @Param("evidence") String evidence,
            @Param("raw") String raw);

    int finish(@Param("id") long id, @Param("token") String token);

    void fail(
            @Param("id") long id,
            @Param("token") String token,
            @Param("retry") boolean retry,
            @Param("code") String code);

    Map<String, Object> resultFor(long documentId);

    List<Map<String, Object>> confirmations(long documentId);

    void confirm(Map<String, Object> confirmation);

    String scenario(long applicationId);

    void ensureAiQuota(String scope);

    int reserveAiQuota(@Param("scope") String scope, @Param("limit") int limit);
}
