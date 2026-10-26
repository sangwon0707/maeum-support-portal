package kr.co.maeum.support.common;

import java.sql.Timestamp;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

/** DATETIME에 저장한 UTC 값을 화면 경계에서만 한국 시간으로 변환한다. */
public final class ViewFormats {
    private static final DateTimeFormatter KOREAN_TIME =
            DateTimeFormatter.ofPattern("yyyy.MM.dd HH:mm");

    private ViewFormats() {}

    public static String koreanTime(Object value) {
        if (value == null) {
            return "";
        }
        LocalDateTime utc;
        if (value instanceof LocalDateTime time) {
            utc = time;
        } else if (value instanceof Timestamp timestamp) {
            utc = timestamp.toLocalDateTime();
        } else {
            throw new IllegalArgumentException("지원하지 않는 시각 형식입니다.");
        }
        return utc.plusHours(9).format(KOREAN_TIME);
    }
}
