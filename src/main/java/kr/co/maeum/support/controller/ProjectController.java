package kr.co.maeum.support.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

@Controller
public class ProjectController {
    @GetMapping("/about")
    public String about() {
        return "about";
    }
}
