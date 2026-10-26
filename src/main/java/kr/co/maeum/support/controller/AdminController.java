package kr.co.maeum.support.controller;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.mapper.AuditMapper;
import kr.co.maeum.support.mapper.IdentityMapper;
import kr.co.maeum.support.mapper.ProgramMapper;
import kr.co.maeum.support.model.Program;
import kr.co.maeum.support.security.PortalPrincipal;
import kr.co.maeum.support.service.ApplicationQueryService;
import kr.co.maeum.support.service.ProgramService;
import kr.co.maeum.support.service.ReviewService;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;

@Controller
public class AdminController {
    private final IdentityMapper identities;
    private final ProgramMapper programs;
    private final ProgramService service;
    private final ApplicationQueryService queries;
    private final ReviewService reviews;
    private final AuditMapper audits;

    public AdminController(
            IdentityMapper identities,
            ProgramMapper programs,
            ProgramService service,
            ApplicationQueryService queries,
            ReviewService reviews,
            AuditMapper audits) {
        this.identities = identities;
        this.programs = programs;
        this.service = service;
        this.queries = queries;
        this.reviews = reviews;
        this.audits = audits;
    }

    @GetMapping("/admin")
    public String admin(@AuthenticationPrincipal PortalPrincipal actor, Model model) {
        model.addAttribute("programs", programs.list("", "", true));
        model.addAttribute(
                "staffAccounts",
                identities.listStaff(actor.workspaceId()).stream()
                        .map(account -> account.principal())
                        .toList());
        model.addAllAttributes(queries.page(actor, 1, "", "", null, null));
        return "admin";
    }

    @GetMapping("/admin/programs/new")
    public String create(Model model) {
        var program = new Program();
        program.setOpensAt(
                LocalDateTime.now(Clock.systemUTC()).plusHours(10).withSecond(0).withNano(0));
        program.setClosesAt(program.getOpensAt().plusDays(30));
        program.setMaxAmount(new BigDecimal("10000000"));
        program.setStatus("DRAFT");
        model.addAttribute("program", program);
        return "program-edit";
    }

    @GetMapping("/admin/programs/{id}")
    public String edit(@PathVariable long id, Model model) {
        var program = programs.find(id);
        if (program == null) {
            throw new BusinessException(404, "공고가 없습니다.");
        }
        program.setOpensAt(program.getOpensAt().plusHours(9));
        program.setClosesAt(program.getClosesAt().plusHours(9));
        model.addAttribute("program", program);
        return "program-edit";
    }

    @PostMapping("/admin/programs")
    public String save(
            @AuthenticationPrincipal PortalPrincipal actor,
            @RequestParam(required = false) Long id,
            @RequestParam(defaultValue = "0") long version,
            @RequestParam String title,
            @RequestParam String body,
            @RequestParam(required = false) String targetText,
            @RequestParam(required = false) String supportText,
            @RequestParam(required = false) String exclusionText,
            @RequestParam(required = false) String selectionText,
            @RequestParam(required = false) String contactText,
            @RequestParam String status,
            @RequestParam BigDecimal maxAmount,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime opensAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
                    LocalDateTime closesAt,
            RedirectAttributes flash) {
        var form = new Program();
        form.setId(id);
        form.setVersion(version);
        form.setTitle(title);
        form.setBody(body);
        form.setTargetText(targetText);
        form.setSupportText(supportText);
        form.setExclusionText(exclusionText);
        form.setSelectionText(selectionText);
        form.setContactText(contactText);
        form.setStatus(status);
        form.setMaxAmount(maxAmount);
        form.setOpensAt(opensAt.minusHours(9));
        form.setClosesAt(closesAt.minusHours(9));
        service.save(actor, form);
        flash.addFlashAttribute("notice", "공고를 저장했습니다.");
        return "redirect:/admin";
    }

    @PostMapping("/admin/assign")
    public String assign(
            @AuthenticationPrincipal PortalPrincipal actor,
            @RequestParam long applicationId,
            @RequestParam long staffId,
            @RequestParam String reason,
            RedirectAttributes flash) {
        reviews.assign(actor, applicationId, staffId, reason);
        flash.addFlashAttribute("notice", "담당자를 배정했습니다.");
        return "redirect:/admin";
    }

    @GetMapping("/admin/audit")
    public String audit(
            @AuthenticationPrincipal PortalPrincipal actor,
            @RequestParam(defaultValue = "") String keyword,
            Model model) {
        model.addAttribute("events", audits.list(actor.workspaceId(), keyword));
        return "audit";
    }
}
