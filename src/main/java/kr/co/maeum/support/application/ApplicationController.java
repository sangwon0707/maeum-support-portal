package kr.co.maeum.support.application;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.document.DocumentMapper;
import kr.co.maeum.support.identity.PortalPrincipal;
import kr.co.maeum.support.intake.IntakeService;
import kr.co.maeum.support.program.ProgramMapper;
import kr.co.maeum.support.review.ReviewMapper;

import org.springframework.beans.BeanUtils;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Controller
public class ApplicationController {
    private final ApplicationService workflow;
    private final ApplicationQueryService queries;
    private final ApplicationMapper applications;
    private final DocumentMapper documents;
    private final ProgramMapper programs;
    private final ReviewMapper reviews;
    private final SampleApplicationService samples;
    private final IntakeService intake;

    public ApplicationController(
            ApplicationService workflow,
            ApplicationQueryService queries,
            ApplicationMapper applications,
            DocumentMapper documents,
            ProgramMapper programs,
            ReviewMapper reviews,
            SampleApplicationService samples,
            IntakeService intake) {
        this.workflow = workflow;
        this.queries = queries;
        this.applications = applications;
        this.documents = documents;
        this.programs = programs;
        this.reviews = reviews;
        this.samples = samples;
        this.intake = intake;
    }

    @PostMapping("/applications")
    public String start(
            @AuthenticationPrincipal PortalPrincipal actor,
            @RequestParam long programId,
            @RequestParam(defaultValue = "FORM") String entry) {
        if (!List.of("FORM", "DOCUMENT").contains(entry))
            throw new BusinessException(422, "신청 방식을 선택해 주세요.");
        long id = workflow.start(actor, programId);
        var status = workflow.view(actor, id).getStatus();
        if ("DOCUMENT".equals(entry) && "DRAFT".equals(status))
            return "redirect:/applications/" + id + "/intake";
        return "redirect:/applications/" + id + editablePath(status);
    }

    private String editablePath(String status) {
        // 중복 신청은 기존 건으로 돌아간다. 제출을 마친 건에는 수정 폼을 노출하지 않는다.
        return "DRAFT".equals(status) || "NEEDS_SUPPLEMENT".equals(status) ? "/edit" : "";
    }

    @GetMapping("/my")
    public String mine(
            @AuthenticationPrincipal PortalPrincipal actor,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "") String status,
            Model model) {
        if ("STAFF".equals(actor.role())) {
            return "redirect:/staff/applications";
        }
        if ("ADMIN".equals(actor.role())) {
            return "redirect:/admin";
        }
        model.addAllAttributes(queries.page(actor, page, "", status, null, null));
        model.addAttribute("selectedStatus", status);
        return "my";
    }

    @GetMapping("/applications/{id}/edit")
    public String edit(
            @AuthenticationPrincipal PortalPrincipal actor, @PathVariable long id, Model model) {
        var application = workflow.view(actor, id);
        if (editablePath(application.getStatus()).isEmpty()) {
            return "redirect:/applications/" + id;
        }
        var form = new ApplicationForm();
        BeanUtils.copyProperties(application, form);
        editModel(model, application, form);
        return "application-edit";
    }

    @PostMapping("/applications/{id}/save")
    public String save(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @ModelAttribute("form") ApplicationForm form,
            BindingResult binding,
            @RequestParam(defaultValue = "stay") String next,
            Model model,
            RedirectAttributes flash) {
        var application = workflow.view(actor, id);
        if (binding.hasErrors()) {
            editModel(model, application, form);
            model.addAttribute("validationError", "날짜·금액 입력 형식을 확인해 주세요.");
            return "application-edit";
        }
        try {
            workflow.save(actor, id, form);
        } catch (BusinessException exception) {
            if (exception.getStatus() != 422) {
                throw exception;
            }
            editModel(model, application, form);
            model.addAttribute("validationError", exception.getMessage());
            return "application-edit";
        }
        flash.addFlashAttribute("notice", "신청 내용을 저장했습니다.");
        return "redirect:/applications/" + id + ("documents".equals(next) ? "/documents" : "/edit");
    }

    private void editModel(Model model, Application application, ApplicationForm form) {
        model.addAttribute("application", application);
        model.addAttribute("form", form);
        var program = programs.find(application.getProgramId());
        model.addAttribute("program", program);
        model.addAttribute("sampleOptions", SampleDatasetPolicy.choices(program));
        var request = reviews.openSupplement(application.getId());
        model.addAttribute("supplement", request);
        model.addAttribute(
                "allowedTargets", request == null ? List.of() : reviews.targets(request.getId()));
    }

    @PostMapping("/applications/{id}/example")
    public String example(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam long version,
            @RequestParam String dataset,
            RedirectAttributes flash) {
        samples.apply(actor, id, version, dataset);
        flash.addFlashAttribute("notice", "예시 신청 정보를 불러와 저장했습니다. 내용을 수정하고 다음 단계에서 서류를 직접 첨부해 주세요.");
        return "redirect:/applications/" + id + "/edit";
    }

    @GetMapping("/applications/{id}/confirm")
    public String confirm(
            @AuthenticationPrincipal PortalPrincipal actor, @PathVariable long id, Model model) {
        var application = workflow.view(actor, id);
        if (!List.of("DRAFT", "NEEDS_SUPPLEMENT").contains(application.getStatus())) {
            return "redirect:/applications/" + id;
        }
        model.addAttribute("application", application);
        model.addAttribute("documents", documents.current(id));
        Long sourceId = applications.appliedIntakeId(id);
        if (sourceId != null) {
            var job = intake.view(actor, id, sourceId);
            model.addAttribute("sourceIntakeId", sourceId);
            model.addAttribute("intakeAnalysis", intake.result(job));
        }
        model.addAttribute("submissionKey", UUID.randomUUID().toString());
        model.addAttribute("supplement", reviews.openSupplement(id));
        return "confirm";
    }

    @PostMapping("/applications/{id}/submit")
    public String submit(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam long version,
            @RequestParam String submissionKey,
            @RequestParam(defaultValue = "false") boolean acknowledged,
            @RequestParam(required = false) String responseText,
            RedirectAttributes flash) {
        if (!acknowledged) {
            throw new BusinessException(422, "신청 내용과 서류 확인에 동의해 주세요.");
        }
        workflow.submit(actor, id, version, submissionKey, responseText);
        flash.addFlashAttribute("notice", "신청을 제출했습니다. 접수 내역을 확인해 주세요.");
        return "redirect:/applications/" + id;
    }

    @GetMapping("/applications/{id}/revisions/{revisionId}")
    public String revision(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @PathVariable long revisionId,
            Model model)
            throws IOException {
        model.addAttribute("application", workflow.view(actor, id));
        var revision = applications.revisionDetail(id, revisionId);
        if (revision == null) {
            throw new BusinessException(404, "제출본을 찾을 수 없습니다.");
        }
        var snapshot =
                new ObjectMapper()
                        .readValue(
                                revision.get("snapshot").toString(),
                                new TypeReference<Map<String, Object>>() {});
        model.addAttribute("revision", revision);
        model.addAttribute("snapshot", snapshot);
        model.addAttribute("documents", applications.revisionDocuments(revisionId));
        return "revision";
    }

    @GetMapping("/applications/{id}")
    public String progress(
            @AuthenticationPrincipal PortalPrincipal actor, @PathVariable long id, Model model) {
        model.addAttribute("application", workflow.view(actor, id));
        model.addAttribute("history", applications.historyList(id));
        model.addAttribute("revisions", applications.revisions(id));
        model.addAttribute("review", reviews.findReview(id));
        model.addAttribute("supplement", reviews.openSupplement(id));
        model.addAttribute("documents", documents.current(id));
        return "progress";
    }
}
