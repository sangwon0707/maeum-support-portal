package kr.co.maeum.support.common;

/** 복구 가능한 업무 오류를 HTTP와 한글 안내로 전달한다. */
public class BusinessException extends RuntimeException {
    private final int status;

    public BusinessException(int status, String message) {
        super(message);
        this.status = status;
    }

    public int getStatus() {
        return status;
    }

    public static BusinessException forbidden() {
        return new BusinessException(403, "접근할 수 없는 업무입니다.");
    }

    public static BusinessException conflict() {
        return new BusinessException(409, "다른 요청에서 변경했습니다. 최신 화면을 다시 열어 주세요.");
    }
}
