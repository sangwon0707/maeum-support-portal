package kr.co.maeum.support.review;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;
import java.util.Map;

@EgovMapper
public interface ReviewMapper {
    void saveReview(Map<String, Object> review);

    Map<String, Object> findReview(long applicationId);

    void insertSupplement(SupplementRequest request);

    void insertItem(Map<String, Object> item);

    SupplementRequest openSupplement(long applicationId);

    List<String> targets(long requestId);

    void answer(Map<String, Object> response);

    void closeSupplement(long requestId);
}
