package kr.co.maeum.support.controller;

import kr.co.maeum.support.config.LegalSettings;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;

@Controller
public class LegalController {
    private final LegalSettings settings;

    public LegalController(LegalSettings settings) {
        this.settings = settings;
    }

    @ModelAttribute("legal")
    public LegalSettings legal() {
        return settings;
    }

    @GetMapping("/terms")
    public String terms() {
        return "terms";
    }

    @GetMapping("/privacy")
    public String privacy() {
        return "privacy";
    }
}
