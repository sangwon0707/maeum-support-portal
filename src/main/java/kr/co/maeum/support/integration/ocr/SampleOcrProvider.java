package kr.co.maeum.support.integration.ocr;

import kr.co.maeum.support.dto.OcrExtraction;
import kr.co.maeum.support.mapper.OcrMapper;
import kr.co.maeum.support.model.Document;

import org.springframework.stereotype.Component;

import java.util.Map;

/** 이미지 인식을 하지 않는 고정 응답이다. 실제 인식률 검증에 사용하면 안 된다. */
@Component
public class SampleOcrProvider implements OcrProvider {
    private final OcrMapper mapper;

    public SampleOcrProvider(OcrMapper mapper) {
        this.mapper = mapper;
    }

    @Override
    public OcrExtraction extract(Document document) {
        String scenario = mapper.scenario(document.getApplicationId());
        if ("OCR_FAIL".equals(scenario)) {
            throw new SampleRecognitionException();
        }
        return new OcrExtraction(
                Map.of(
                        "businessName",
                        "MISMATCH".equals(scenario) ? "가상문구점" : "가상상점",
                        "businessNo",
                        "0000000000",
                        "representative",
                        "가상대표",
                        "openedOn",
                        "2024-01-01"),
                Map.of(),
                Map.of("engine", "sample", "modelVersion", "sample-v1"));
    }

    public static class SampleRecognitionException extends RuntimeException {}
}
