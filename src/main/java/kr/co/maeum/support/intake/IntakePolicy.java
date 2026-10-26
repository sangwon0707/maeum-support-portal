package kr.co.maeum.support.intake;

import kr.co.maeum.support.common.BusinessException;

import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.util.List;

@Component
public class IntakePolicy {
    public static final List<String> KINDS =
            List.of(
                    "AUTO",
                    "APPLICATION",
                    "BUSINESS_PLAN",
                    "BUSINESS_CERT",
                    "ESTIMATE",
                    "PHOTO_SHEET",
                    "CONSENT",
                    "OTHER");

    public void files(String layout, int count, List<String> kinds) {
        if (!List.of("BUNDLE", "SEPARATE").contains(layout)
                || count < 1
                || count > 5
                || kinds.size() != count)
            throw new BusinessException(422, "파일 구성과 종류를 확인해 주세요. 최대 5개까지 선택할 수 있습니다.");
        if ("BUNDLE".equals(layout) && count != 1)
            throw new BusinessException(422, "통합 서류는 한 파일로 선택해 주세요.");
        if (kinds.stream().anyMatch(kind -> !KINDS.contains(kind)))
            throw new BusinessException(422, "문서 종류를 확인해 주세요.");
    }

    public boolean isPdf(byte[] bytes) {
        if (bytes.length == 0 || bytes.length > 10 * 1024 * 1024)
            throw new BusinessException(422, "파일당 10MiB 이하로 선택해 주세요.");
        return bytes.length >= 5
                && "%PDF-".equals(new String(bytes, 0, 5, StandardCharsets.US_ASCII));
    }

    public static String label(String kind) {
        return switch (kind) {
            case "APPLICATION" -> "신청서";
            case "BUSINESS_PLAN" -> "사업계획서";
            case "BUSINESS_CERT" -> "사업자등록증";
            case "ESTIMATE" -> "견적서";
            case "PHOTO_SHEET" -> "현장 사진";
            case "CONSENT" -> "동의서";
            case "AUTO" -> "자동 분류";
            default -> "기타·확인 필요";
        };
    }
}
