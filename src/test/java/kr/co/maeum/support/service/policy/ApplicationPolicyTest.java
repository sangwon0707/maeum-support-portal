package kr.co.maeum.support.service.policy;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import kr.co.maeum.support.common.BusinessException;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.LocalDateTime;

class ApplicationPolicyTest {
    private final ApplicationPolicy policy = new ApplicationPolicy();

    @Test
    @DisplayName("접수 시작 시각은 허용하고 마감 시각부터 차단한다")
    void deadline_accepts_open_and_rejects_close() {
        var open = LocalDateTime.of(2026, 10, 1, 0, 0);
        var close = open.plusDays(1);
        assertDoesNotThrow(() -> policy.deadline(open, open, close));
        assertThrows(BusinessException.class, () -> policy.deadline(close, open, close));
        assertThrows(
                BusinessException.class, () -> policy.deadline(open.minusNanos(1), open, close));
    }
}
