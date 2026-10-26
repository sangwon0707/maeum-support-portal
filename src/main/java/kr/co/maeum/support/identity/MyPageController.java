package kr.co.maeum.support.identity;

import kr.co.maeum.support.application.ApplicationQueryService;

import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class MyPageController {
    private final ApplicationQueryService applications;

    public MyPageController(ApplicationQueryService applications) {
        this.applications = applications;
    }

    @GetMapping("/mypage")
    public String page(@AuthenticationPrincipal PortalPrincipal actor, Model model) {
        if (!"APPLICANT".equals(actor.role())) {
            return "redirect:/my";
        }
        model.addAttribute("summary", applications.mySummary(actor));
        model.addAttribute("publicAccount", "applicant".equals(actor.username()) && !actor.demo());
        return "mypage";
    }
}
