package kr.co.maeum.support.identity;

import kr.co.maeum.support.common.BusinessException;

import org.springframework.dao.DuplicateKeyException;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;

/** 공개 가입으로는 신청자 계정만 만든다. 담당자·관리자 권한은 가입 입력으로 받지 않는다. */
@Service
public class RegistrationService {
    private final IdentityMapper identities;
    private final BCryptPasswordEncoder encoder;

    public RegistrationService(IdentityMapper identities, BCryptPasswordEncoder encoder) {
        this.identities = identities;
        this.encoder = encoder;
    }

    @Transactional
    public void register(String loginId, String displayName, String password, String confirmation) {
        if (loginId == null || !loginId.matches("[a-z][a-z0-9_-]{3,29}")) {
            throw new BusinessException(422, "아이디는 영문 소문자로 시작하는 4~30자의 영문·숫자·밑줄·하이픈으로 입력해 주세요.");
        }
        if (displayName == null || displayName.isBlank() || displayName.strip().length() > 50) {
            throw new BusinessException(422, "표시 이름을 50자 이내로 입력해 주세요.");
        }
        if (password == null
                || password.length() < 10
                || password.getBytes(StandardCharsets.UTF_8).length > 72
                || !password.equals(confirmation)) {
            throw new BusinessException(422, "비밀번호는 10자 이상, UTF-8 72바이트 이하로 입력하고 확인값을 일치시켜 주세요.");
        }
        var account = new HashMap<String, Object>();
        account.put("workspaceId", 1L);
        account.put("loginId", loginId);
        account.put("displayName", displayName.strip());
        account.put("passwordHash", encoder.encode(password));
        try {
            identities.insertAccount(account);
            identities.insertRole(((Number) account.get("id")).longValue(), "APPLICANT");
        } catch (DuplicateKeyException exception) {
            throw new BusinessException(409, "사용 중인 아이디입니다. 다른 아이디를 입력해 주세요.");
        }
    }
}
