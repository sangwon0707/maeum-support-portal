package kr.co.maeum.support.ocr;

import kr.co.maeum.support.document.Document;

/** 실제 공급자 추가 시 업무 흐름을 바꾸지 않기 위한 연동 경계다. */
public interface OcrProvider {
    OcrExtraction extract(Document document);
}
