package kr.co.maeum.support.integration.ocr;

public class OcrProcessingException extends RuntimeException {
    private final String code;
    private final boolean retryable;

    public OcrProcessingException(String code, boolean retryable) {
        super(code);
        this.code = code;
        this.retryable = retryable;
    }

    public String code() {
        return code;
    }

    public boolean retryable() {
        return retryable;
    }
}
