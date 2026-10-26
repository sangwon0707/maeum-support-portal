package kr.co.maeum.support.application;

import kr.co.maeum.support.common.BusinessException;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

/** DB나 인증 객체 없이 검증할 수 있는 업무 규칙을 모은다. */
@Component
public class ApplicationPolicy {
    public void expectVersion(Application application, long expectedVersion) {
        if (application.getVersion() != expectedVersion) {
            throw BusinessException.conflict();
        }
    }

    public void expectStatus(Application application, ApplicationStatus status) {
        if (!status.name().equals(application.getStatus())) {
            throw new BusinessException(409, "현재 상태에서는 이 작업을 할 수 없습니다.");
        }
    }

    public void deadline(LocalDateTime now, LocalDateTime opensAt, LocalDateTime closesAt) {
        if (now.isBefore(opensAt) || !now.isBefore(closesAt)) {
            throw new BusinessException(422, "접수 또는 보완 기한을 확인해 주세요.");
        }
    }

    public void validate(
            Application application, BigDecimal maxAmount, boolean submitting, LocalDate today) {
        text(application.getAddress(), "사업장 주소", 300, submitting);
        text(application.getBusinessType(), "업태", 100, submitting);
        text(application.getBusinessItem(), "종목", 100, submitting);
        var contribution = application.getSelfContribution();
        if ((submitting && contribution == null)
                || (contribution != null
                        && (contribution.signum() < 0
                                || contribution.scale() > 0
                                || contribution.compareTo(new BigDecimal("100000000")) > 0))) {
            throw new BusinessException(422, "자부담은 0원 이상 1억원 이하의 정수로 입력해 주세요.");
        }
        text(application.getBusinessName(), "상호", 100, submitting);
        text(application.getRepresentative(), "대표자", 50, submitting);
        text(application.getPlanText(), "개선 계획", 2000, submitting);
        String number = application.getBusinessNo();
        if (submitting || number != null && !number.isBlank()) {
            if (number == null || !number.matches("[0-9]{10}")) {
                throw new BusinessException(422, "사업자등록번호는 숫자 10자리로 입력해 주세요.");
            }
        }
        String phone = application.getPhone();
        if (submitting || phone != null && !phone.isBlank()) {
            if (phone == null || !phone.matches("[0-9-]{9,20}")) {
                throw new BusinessException(422, "연락처 형식을 확인해 주세요.");
            }
        }
        if (submitting && application.getOpenedOn() == null
                || application.getOpenedOn() != null && application.getOpenedOn().isAfter(today)) {
            throw new BusinessException(422, "개업일은 오늘 이전의 날짜로 입력해 주세요.");
        }
        BigDecimal amount = application.getRequestedAmount();
        if (submitting && amount == null
                || amount != null
                        && (amount.scale() > 0
                                || amount.signum() <= 0
                                || amount.compareTo(maxAmount) > 0)) {
            throw new BusinessException(422, "신청금액은 지원 한도 이내의 정수 원화로 입력해 주세요.");
        }
    }

    public void text(String value, String label, int maxLength, boolean required) {
        if (required && (value == null || value.isBlank())) {
            throw new BusinessException(422, label + " 항목을 입력해 주세요.");
        }
        if (value != null && value.length() > maxLength) {
            throw new BusinessException(422, label + " 항목은 " + maxLength + "자 이내로 입력해 주세요.");
        }
    }
}
