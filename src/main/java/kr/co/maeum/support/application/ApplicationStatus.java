package kr.co.maeum.support.application;

public enum ApplicationStatus {
    DRAFT("작성 중"),
    SUBMITTED("접수 완료"),
    UNDER_REVIEW("검토 중"),
    NEEDS_SUPPLEMENT("보완 요청"),
    APPROVED("선정"),
    REJECTED("미선정");
    private final String label;

    ApplicationStatus(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
