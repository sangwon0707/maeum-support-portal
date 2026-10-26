package kr.co.maeum.support.controller;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.dto.ApplicationForm;
import kr.co.maeum.support.mapper.ProgramMapper;
import kr.co.maeum.support.security.PortalPrincipal;
import kr.co.maeum.support.service.ApplicationService;
import kr.co.maeum.support.service.IntakeService;
import kr.co.maeum.support.service.policy.IntakePolicy;
import kr.co.maeum.support.service.policy.SampleDatasetPolicy;

import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.ResponseBody;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Controller
public class IntakeController {
    private final IntakeService intake;
    private final ApplicationService workflow;
    private final ProgramMapper programs;

    public IntakeController(
            IntakeService intake, ApplicationService workflow, ProgramMapper programs) {
        this.intake = intake;
        this.workflow = workflow;
        this.programs = programs;
    }

    @GetMapping("/applications/{id}/intake")
    public String page(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam(required = false) Long intakeId,
            Model model) {
        var application = workflow.view(actor, id);
        var job = intake.view(actor, id, intakeId);
        var result = intake.result(job);
        boolean editable =
                "APPLICANT".equals(actor.role())
                        && actor.id() == application.getOwnerId()
                        && "DRAFT".equals(application.getStatus())
                        && !actor.demo();
        model.addAttribute("application", application);
        model.addAttribute("intake", job);
        model.addAttribute("analysis", result);
        model.addAttribute("preparedAvailable", intake.preparedAvailable(job));
        model.addAttribute("liveTestKey", java.util.UUID.randomUUID().toString());
        model.addAttribute("canUpload", editable);
        model.addAttribute(
                "canStartAnalysis",
                editable && job != null && "UPLOADED".equals(job.get("status")));
        model.addAttribute(
                "activeAnalysis",
                editable && job != null && "UPLOADED".equals(job.get("status"))
                        ? intake.activeForProgram(actor, application.getProgramId())
                        : Map.of("active", false));
        model.addAttribute(
                "canRevise", editable && job != null && "APPLIED".equals(job.get("status")));
        model.addAttribute(
                "canApply", editable && job != null && "READY".equals(job.get("status")));
        model.addAttribute(
                "form",
                intake.form(
                        application, result, job != null && "APPLIED".equals(job.get("status"))));
        model.addAttribute(
                "kinds",
                IntakePolicy.KINDS.stream()
                        .filter(kind -> !"AUTO".equals(kind))
                        .map(kind -> Map.of("code", kind, "label", IntakePolicy.label(kind)))
                        .toList());
        model.addAttribute(
                "sources",
                job == null ? List.of() : intake.sources(((Number) job.get("id")).longValue()));
        model.addAttribute(
                "sampleOptions",
                SampleDatasetPolicy.choices(programs.find(application.getProgramId())).stream()
                        .filter(
                                option ->
                                        getClass()
                                                        .getResource(
                                                                "/fixtures/"
                                                                        + option.code()
                                                                        + "/application.pdf")
                                                != null)
                        .toList());
        model.addAttribute(
                "formFields",
                List.of(
                        Map.of("name", "businessName", "label", "상호", "type", "text"),
                        Map.of("name", "businessNo", "label", "사업자등록번호 (숫자 10자리)", "type", "text"),
                        Map.of("name", "representative", "label", "대표자", "type", "text"),
                        Map.of("name", "openedOn", "label", "개업일", "type", "date"),
                        Map.of("name", "phone", "label", "연락처", "type", "text"),
                        Map.of("name", "address", "label", "사업장 주소", "type", "text"),
                        Map.of("name", "businessType", "label", "업태", "type", "text"),
                        Map.of("name", "businessItem", "label", "종목", "type", "text"),
                        Map.of("name", "requestedAmount", "label", "신청금액 (원)", "type", "number"),
                        Map.of("name", "selfContribution", "label", "자부담 (원)", "type", "number")));
        return "intake";
    }

    @GetMapping("/programs/{programId}/intake/active")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> active(
            @AuthenticationPrincipal PortalPrincipal actor, @PathVariable long programId) {
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(intake.activeForProgram(actor, programId));
    }

    @PostMapping("/programs/{programId}/intake")
    public String startUpload(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long programId,
            @RequestParam String layout,
            @RequestParam List<String> kinds,
            @RequestParam List<MultipartFile> files,
            @RequestParam(required = false) String requestKey)
            throws IOException {
        if (files.isEmpty() || files.size() != kinds.size()) {
            throw new BusinessException(422, "분석할 파일을 선택해 주세요.");
        }
        var uploads = new ArrayList<IntakeService.Upload>();
        for (int index = 0; index < files.size(); index++) {
            uploads.add(
                    new IntakeService.Upload(
                            files.get(index).getOriginalFilename(),
                            kinds.get(index),
                            files.get(index).getBytes()));
        }
        long applicationId = workflow.start(actor, programId);
        long intakeId = intake.upload(actor, applicationId, layout, uploads, requestKey);
        return "redirect:/applications/" + applicationId + "/intake?intakeId=" + intakeId;
    }

    @PostMapping("/applications/{id}/intake")
    public String upload(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam String layout,
            @RequestParam List<String> kinds,
            @RequestParam List<MultipartFile> files,
            @RequestParam(required = false) String requestKey)
            throws IOException {
        if (files.size() != kinds.size()) throw new BusinessException(422, "파일별 문서 종류를 확인해 주세요.");
        var uploads = new ArrayList<IntakeService.Upload>();
        for (int index = 0; index < files.size(); index++)
            uploads.add(
                    new IntakeService.Upload(
                            files.get(index).getOriginalFilename(),
                            kinds.get(index),
                            files.get(index).getBytes()));
        long intakeId = intake.upload(actor, id, layout, uploads, requestKey);
        return "redirect:/applications/" + id + "/intake?intakeId=" + intakeId;
    }

    @GetMapping("/applications/{id}/intake/status")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> status(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam long intakeId) {
        var job = intake.view(actor, id, intakeId);
        if (job == null) throw new BusinessException(404, "분석 작업을 찾을 수 없습니다.");
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .body(
                        Map.of(
                                "status",
                                job.get("status"),
                                "stage",
                                job.get("stage"),
                                "progress",
                                job.get("progress"),
                                "detail",
                                job.get("detail")));
    }

    @GetMapping("/intakes/{id}/assets/{key}")
    public ResponseEntity<byte[]> asset(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @PathVariable String key)
            throws IOException {
        var path = intake.asset(actor, id, key);
        return ResponseEntity.ok()
                .cacheControl(CacheControl.noStore())
                .contentType(key.endsWith(".pdf") ? MediaType.APPLICATION_PDF : MediaType.IMAGE_PNG)
                .header("X-Content-Type-Options", "nosniff")
                .header(
                        "Content-Disposition",
                        org.springframework.http.ContentDisposition.inline()
                                .filename(
                                        intake.authorizedFilename(actor, id, key),
                                        java.nio.charset.StandardCharsets.UTF_8)
                                .build()
                                .toString())
                // 소유권을 검사한 원본만 같은 사이트의 확인 화면에 표시한다.
                .header("X-Frame-Options", "SAMEORIGIN")
                .header("Content-Security-Policy", "default-src 'none'; frame-ancestors 'self'")
                .body(Files.readAllBytes(path));
    }

    @PostMapping("/applications/{id}/intake/{intakeId}/start")
    public String startAnalysis(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @PathVariable long intakeId,
            @RequestParam(defaultValue = "PREPARED") String mode,
            @RequestParam(defaultValue = "false") boolean slowAcknowledged) {
        intake.startAnalysis(actor, id, intakeId, mode, slowAcknowledged);
        return "redirect:/applications/" + id + "/intake?intakeId=" + intakeId;
    }

    @PostMapping("/applications/{id}/intake/{intakeId}/live-test")
    public String liveTest(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @PathVariable long intakeId,
            @RequestParam String requestKey,
            @RequestParam(defaultValue = "false") boolean slowAcknowledged) {
        long next = intake.requestLiveTest(actor, id, intakeId, requestKey, slowAcknowledged);
        return "redirect:/applications/" + id + "/intake?intakeId=" + next;
    }

    @PostMapping("/applications/{id}/intake/{intakeId}/save")
    public String save(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @PathVariable long intakeId,
            @ModelAttribute ApplicationForm form,
            BindingResult binding,
            RedirectAttributes flash) {
        if (binding.hasErrors()) {
            throw new BusinessException(422, "날짜와 사업비 입력 형식을 확인해 주세요.");
        }
        intake.saveReviewedContent(actor, id, intakeId, form);
        flash.addFlashAttribute("notice", "수정한 신청 내용을 저장했습니다.");
        return "redirect:/applications/" + id + "/intake?intakeId=" + intakeId;
    }

    @PostMapping("/applications/{id}/intake/{intakeId}/apply")
    public String apply(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @PathVariable long intakeId,
            @ModelAttribute ApplicationForm form,
            BindingResult binding,
            @RequestParam List<String> pageKinds,
            @RequestParam(required = false, defaultValue = "") List<Integer> groupStarts,
            @RequestParam(required = false, defaultValue = "") List<String> photoKeys,
            RedirectAttributes flash) {
        if (binding.hasErrors()) throw new BusinessException(422, "날짜와 사업비 입력 형식을 확인해 주세요.");
        boolean complete =
                intake.apply(actor, id, intakeId, form, pageKinds, groupStarts, photoKeys);
        flash.addFlashAttribute("notice", "검토한 신청 내용·분할 서류·사진을 반영했습니다.");
        return "redirect:/applications/" + id + (complete ? "/confirm" : "/documents");
    }
}
