package kr.co.maeum.support.legal;

import kr.co.maeum.support.config.DatabaseConfig;

import org.springframework.stereotype.Component;

/** 운영자가 확정한 공개 정보만 표시한다. 개발자 이름이나 연락처를 임의로 추정하지 않는다. */
@Component
public class LegalSettings {
    public String getOperatorName() {
        return DatabaseConfig.env("LEGAL_OPERATOR_NAME", "").strip();
    }

    public String getContactEmail() {
        String email = DatabaseConfig.env("LEGAL_CONTACT_EMAIL", "").strip();
        return email.matches("[A-Za-z0-9._%+-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,63}") ? email : "";
    }

    public String getHostingProvider() {
        return DatabaseConfig.env("LEGAL_HOSTING_PROVIDER", "").strip();
    }

    public boolean isContactConfigured() {
        return !getOperatorName().isBlank() && !getContactEmail().isBlank();
    }
}
