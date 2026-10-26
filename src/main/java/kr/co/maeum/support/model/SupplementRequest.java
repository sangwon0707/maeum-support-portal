package kr.co.maeum.support.model;

import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class SupplementRequest {
    private Long id;
    private Long applicationId;
    private Long requestedBy;
    private String reason;
    private LocalDateTime dueAt;
    private String status;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Long getApplicationId() {
        return applicationId;
    }

    public void setApplicationId(Long applicationId) {
        this.applicationId = applicationId;
    }

    public Long getRequestedBy() {
        return requestedBy;
    }

    public void setRequestedBy(Long requestedBy) {
        this.requestedBy = requestedBy;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public LocalDateTime getDueAt() {
        return dueAt;
    }

    public void setDueAt(LocalDateTime dueAt) {
        this.dueAt = dueAt;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getDueAtLabel() {
        return dueAt == null
                ? ""
                : dueAt.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
    }
}
