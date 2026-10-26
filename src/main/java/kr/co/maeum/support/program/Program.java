package kr.co.maeum.support.program;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class Program {
    private Long id;
    private String seedKey;

    public String getSeedKey() {
        return seedKey;
    }

    public void setSeedKey(String seedKey) {
        this.seedKey = seedKey;
    }

    private String title;
    private String body;
    private String status;
    private LocalDateTime opensAt;
    private LocalDateTime closesAt;
    private BigDecimal maxAmount;
    private Long version;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getBody() {
        return body;
    }

    public void setBody(String body) {
        this.body = body;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public LocalDateTime getOpensAt() {
        return opensAt;
    }

    public void setOpensAt(LocalDateTime opensAt) {
        this.opensAt = opensAt;
    }

    public LocalDateTime getClosesAt() {
        return closesAt;
    }

    public void setClosesAt(LocalDateTime closesAt) {
        this.closesAt = closesAt;
    }

    public BigDecimal getMaxAmount() {
        return maxAmount;
    }

    public void setMaxAmount(BigDecimal maxAmount) {
        this.maxAmount = maxAmount;
    }

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public String getOpensAtLabel() {
        return opensAt == null
                ? ""
                : opensAt.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
    }

    public String getClosesAtLabel() {
        return closesAt == null
                ? ""
                : closesAt.plusHours(9).format(DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm"));
    }

    public boolean isOpen() {
        LocalDateTime now = LocalDateTime.now(Clock.systemUTC());
        return "PUBLISHED".equals(status) && !now.isBefore(opensAt) && now.isBefore(closesAt);
    }

    private String targetText;

    public String getTargetText() {
        return targetText;
    }

    public void setTargetText(String targetText) {
        this.targetText = targetText;
    }

    private String supportText;

    public String getSupportText() {
        return supportText;
    }

    public void setSupportText(String supportText) {
        this.supportText = supportText;
    }

    private String exclusionText;

    public String getExclusionText() {
        return exclusionText;
    }

    public void setExclusionText(String exclusionText) {
        this.exclusionText = exclusionText;
    }

    private String selectionText;

    public String getSelectionText() {
        return selectionText;
    }

    public void setSelectionText(String selectionText) {
        this.selectionText = selectionText;
    }

    private String contactText;

    public String getContactText() {
        return contactText;
    }

    public void setContactText(String contactText) {
        this.contactText = contactText;
    }
}
