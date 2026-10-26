package kr.co.maeum.support.identity;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/** 공개 포트폴리오의 가입 화면은 입력 예시만 제공하며 계정 생성 경로를 노출하지 않는다. */
@Controller
public class RegistrationController {
    @GetMapping("/register")
    public String form() {
        return "register";
    }
}
