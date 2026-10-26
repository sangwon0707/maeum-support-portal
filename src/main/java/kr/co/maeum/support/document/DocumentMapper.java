package kr.co.maeum.support.document;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;

@EgovMapper
public interface DocumentMapper {
    Document find(long id);

    List<Document> current(long applicationId);

    void insert(Document document);
}
