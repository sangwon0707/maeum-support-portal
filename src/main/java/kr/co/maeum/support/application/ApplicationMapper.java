package kr.co.maeum.support.application;

import kr.co.maeum.support.document.Document;

import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;
import java.util.Map;

@EgovMapper
public interface ApplicationMapper {
    Application find(long id);

    Long appliedIntakeId(long applicationId);

    Application lock(long id);

    Application findOwned(
            @Param("workspaceId") long workspaceId,
            @Param("ownerId") long ownerId,
            @Param("programId") long programId);

    List<Application> list(Map<String, Object> filter);

    long count(Map<String, Object> filter);

    List<Map<String, Object>> statusCounts(Map<String, Object> filter);

    int isAssigned(@Param("applicationId") long applicationId, @Param("staffId") long staffId);

    void insert(Application application);

    Long lockOwner(long ownerId);

    void insertPracticeRound(Application application);

    int update(Application application);

    void assign(Map<String, Object> assignment);

    void history(Map<String, Object> event);

    List<Map<String, Object>> historyList(long applicationId);

    void revision(Map<String, Object> revision);

    void revisionDocument(
            @Param("revisionId") long revisionId, @Param("documentId") long documentId);

    List<Map<String, Object>> revisions(long applicationId);

    Map<String, Object> revisionDetail(
            @Param("applicationId") long applicationId, @Param("revisionId") long revisionId);

    List<Document> revisionDocuments(long revisionId);

    Long submittedCommand(
            @Param("applicationId") long applicationId,
            @Param("key") String key,
            @Param("hash") String hash);

    int commandExists(@Param("applicationId") long applicationId, @Param("key") String key);

    int changedDocument(@Param("applicationId") long applicationId, @Param("type") String type);

    void command(Map<String, Object> command);
}
