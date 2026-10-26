package kr.co.maeum.support.controller;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.mapper.ProgramMapper;
import kr.co.maeum.support.service.policy.SampleDatasetPolicy;

import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;

@Controller
public class ProgramController {
    private final ProgramMapper programs;

    public ProgramController(ProgramMapper programs) {
        this.programs = programs;
    }

    @GetMapping({"/", "/programs"})
    public String list(
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String state,
            Model model) {
        model.addAttribute("programs", programs.list(keyword, state, false));
        model.addAttribute("keyword", keyword);
        model.addAttribute("state", state);
        return "programs";
    }

    @GetMapping("/programs/{id}")
    public String detail(@PathVariable long id, Model model) {
        var program = programs.find(id);
        if (program == null || "DRAFT".equals(program.getStatus())) {
            throw new BusinessException(404, "공고를 찾을 수 없습니다.");
        }
        model.addAttribute("program", program);
        model.addAttribute("sampleOptions", SampleDatasetPolicy.choices(program));
        return "program";
    }

    @GetMapping("/guide")
    public String guide(Model model) {
        model.addAttribute("sampleOptions", SampleDatasetPolicy.all());
        return "guide";
    }

    @GetMapping("/login")
    public String login() {
        return "login";
    }

    @GetMapping("/health")
    @ResponseBody
    public String health() {
        return "ok";
    }
}
