package kr.co.maeum.support.mapper;

import kr.co.maeum.support.model.Document;

import org.egovframe.rte.psl.dataaccess.mapper.EgovMapper;

import java.util.List;

@EgovMapper
public interface DocumentMapper {
    Document find(long id);

    List<Document> current(long applicationId);

    void insert(Document document);
}
