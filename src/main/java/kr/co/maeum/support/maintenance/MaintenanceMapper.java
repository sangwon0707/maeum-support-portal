package kr.co.maeum.support.maintenance;

import org.apache.ibatis.annotations.Param;
import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.time.LocalDateTime;
import java.util.List;

@EgovMapper
public interface MaintenanceMapper {
    List<Long> candidates(@Param("login") String login, @Param("cutoff") LocalDateTime cutoff);

    Long lockCandidate(
            @Param("id") long id,
            @Param("login") String login,
            @Param("cutoff") LocalDateTime cutoff);

    int activeJobs(long id);

    List<String> documentKeys();

    List<String> intakeKeys();

    List<String> resultPayloads();

    List<String> cachePayloads();

    void expireCache(@Param("login") String login, @Param("cutoff") LocalDateTime cutoff);

    int deleteConfirmations(long id);

    int deleteOcrResults(long id);

    int deleteOcrJobs(long id);

    int deleteRevisionDocuments(long id);

    int deleteSupplementResponses(long id);

    int deleteSupplementItems(long id);

    int deleteSupplements(long id);

    int deleteCommands(long id);

    int deleteRevisions(long id);

    int deleteDocuments(long id);

    int deleteIntakeSources(long id);

    int deleteIntakes(long id);

    int deleteReviews(long id);

    int deleteAssignments(long id);

    int deleteHistory(long id);

    int deleteAudit(long id);

    int deleteApplication(long id);
}
