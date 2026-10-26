package kr.co.maeum.support.ocr;

import java.util.Map;

/** 인식 원문·항목 후보·출처는 사용자 확인값과 별도로 보존한다. */
public record OcrExtraction(Map<String, String> fields, Map<String, Object> evidence, Object raw) {}
