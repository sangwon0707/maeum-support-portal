package kr.co.maeum.support.review;

import kr.co.maeum.support.application.ApplicationMapper;
import kr.co.maeum.support.application.ApplicationQueryService;
import kr.co.maeum.support.application.ApplicationService;
import kr.co.maeum.support.document.DocumentMapper;
import kr.co.maeum.support.identity.PortalPrincipal;
import kr.co.maeum.support.reporting.ReportingMapper;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Controller
public class ReviewController {
    private final ApplicationQueryService queries;
    private final ApplicationService workflow;
    private final ReviewService reviews;
    private final DocumentMapper documents;
    private final ApplicationMapper applications;
    private final ReviewMapper reviewMapper;
    private final ReportingMapper reports;

    public ReviewController(
            ApplicationQueryService queries,
            ApplicationService workflow,
            ReviewService reviews,
            DocumentMapper documents,
            ApplicationMapper applications,
            ReviewMapper reviewMapper,
            ReportingMapper reports) {
        this.queries = queries;
        this.workflow = workflow;
        this.reviews = reviews;
        this.documents = documents;
        this.applications = applications;
        this.reviewMapper = reviewMapper;
        this.reports = reports;
    }

    @GetMapping("/staff/applications")
    public String list(
            @AuthenticationPrincipal PortalPrincipal actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "") String keyword,
            @RequestParam(defaultValue = "") String status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate from,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
                    LocalDate until,
            Model model) {
        model.addAllAttributes(queries.page(actor, page, keyword, status, from, until));
        return "staff-list";
    }

    @GetMapping("/staff/applications/{id}")
    public String detail(
            @AuthenticationPrincipal PortalPrincipal actor, @PathVariable long id, Model model) {
        model.addAttribute("application", workflow.view(actor, id));
        model.addAttribute("documents", documents.current(id));
        model.addAttribute("history", applications.historyList(id));
        model.addAttribute("review", reviewMapper.findReview(id));
        model.addAttribute("supplement", reviewMapper.openSupplement(id));
        model.addAttribute(
                "defaultDueAt",
                LocalDateTime.now(Clock.systemUTC())
                        .plusHours(9)
                        .plusDays(3)
                        .withSecond(0)
                        .withNano(0));
        return "staff-review";
    }

    @PostMapping("/staff/applications/{id}/start")
    public String start(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam long version) {
        reviews.start(actor, id, version);
        return "redirect:/staff/applications/" + id;
    }

    @PostMapping("/staff/applications/{id}/supplement")
    public String supplement(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam long version,
            @RequestParam String target,
            @RequestParam String reason,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) LocalDateTime dueAt,
            RedirectAttributes flash) {
        reviews.supplement(actor, id, version, target, reason, dueAt.minusHours(9));
        flash.addFlashAttribute("notice", "보완을 요청했습니다.");
        return "redirect:/staff/applications/" + id;
    }

    @PostMapping("/staff/applications/{id}/decision")
    public String decision(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam long version,
            @RequestParam String decision,
            @RequestParam String reason,
            @RequestParam(defaultValue = "false") boolean checked,
            RedirectAttributes flash) {
        reviews.decide(actor, id, version, decision, reason, checked);
        flash.addFlashAttribute("notice", "결과를 확정했습니다.");
        return "redirect:/staff/applications/" + id;
    }

    @GetMapping({"/staff/reports", "/admin/reports"})
    public String reports(@AuthenticationPrincipal PortalPrincipal actor, Model model) {
        var scope = queries.scope(actor);
        model.addAttribute("statusCounts", reports.statusCounts(scope));
        model.addAttribute("staffCounts", reports.staffCounts(scope));
        model.addAttribute("overdue", reports.overdue(scope));
        return "reports";
    }
}
