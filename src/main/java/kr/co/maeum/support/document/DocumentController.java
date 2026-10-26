package kr.co.maeum.support.document;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.maeum.support.application.ApplicationService;
import kr.co.maeum.support.application.SampleDatasetPolicy;
import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.identity.PortalPrincipal;
import kr.co.maeum.support.ocr.OcrMapper;
import kr.co.maeum.support.ocr.OcrService;
import kr.co.maeum.support.ocr.OcrSettings;
import kr.co.maeum.support.program.ProgramMapper;

import org.springframework.http.ContentDisposition;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.nio.file.Files;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Controller
public class DocumentController {
    private final DocumentService documents;
    private final DocumentMapper mapper;
    private final ApplicationService workflow;
    private final OcrService ocr;
    private final OcrMapper jobs;
    private final OcrSettings settings;
    private final ProgramMapper programs;

    public DocumentController(
            DocumentService documents,
            DocumentMapper mapper,
            ApplicationService workflow,
            OcrService ocr,
            OcrMapper jobs,
            OcrSettings settings,
            ProgramMapper programs) {
        this.documents = documents;
        this.mapper = mapper;
        this.workflow = workflow;
        this.ocr = ocr;
        this.jobs = jobs;
        this.settings = settings;
        this.programs = programs;
    }

    @GetMapping("/samples/{dataset}/{kind}")
    public ResponseEntity<byte[]> sample(@PathVariable String dataset, @PathVariable String kind)
            throws IOException {
        if (!SampleDatasetPolicy.exists(dataset)
                || !List.of("business-cert", "estimate", "application", "application-scan")
                        .contains(kind)) {
            throw new BusinessException(404, "예시 서류가 없습니다.");
        }
        try (var stream =
                getClass()
                        .getResourceAsStream(
                                "/fixtures/"
                                        + dataset
                                        + "/"
                                        + kind
                                        + (kind.startsWith("application") ? ".pdf" : ".png"))) {
            if (stream == null) {
                throw new BusinessException(404, "예시 서류가 없습니다.");
            }
            return ResponseEntity.ok()
                    .contentType(
                            kind.startsWith("application")
                                    ? MediaType.APPLICATION_PDF
                                    : MediaType.IMAGE_PNG)
                    .header(
                            "Content-Disposition",
                            ContentDisposition.attachment()
                                    .filename(
                                            dataset
                                                    + "-"
                                                    + kind
                                                    + (kind.startsWith("application")
                                                            ? ".pdf"
                                                            : ".png"))
                                    .build()
                                    .toString())
                    .body(stream.readAllBytes());
        }
    }

    @GetMapping("/applications/{id}/documents")
    public String list(
            @AuthenticationPrincipal PortalPrincipal actor, @PathVariable long id, Model model) {
        var application = workflow.view(actor, id);
        model.addAttribute("application", application);
        model.addAttribute(
                "sampleOptions",
                SampleDatasetPolicy.choices(programs.find(application.getProgramId())));
        model.addAttribute("documents", mapper.current(id));
        model.addAttribute("editableTypes", documents.editableTypes(actor, id));
        model.addAttribute("pendingReplacementTypes", documents.pendingReplacementTypes(actor, id));
        return "documents";
    }

    @PostMapping("/applications/{id}/documents/fixture")
    public String fixture(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam String type,
            @RequestParam(defaultValue = "original") String dataset,
            RedirectAttributes flash) {
        documents.attachFixture(actor, id, type, dataset);
        flash.addFlashAttribute("notice", "가상 서류를 첨부했습니다. 이전 제출본은 유지됩니다.");
        return "redirect:/applications/" + id + "/documents";
    }

    @PostMapping("/applications/{id}/documents/upload")
    public String upload(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam String type,
            @RequestParam MultipartFile file,
            RedirectAttributes flash)
            throws IOException {
        documents.upload(actor, id, type, file.getOriginalFilename(), file.getBytes());
        flash.addFlashAttribute("notice", "서류를 첨부했습니다.");
        return "redirect:/applications/" + id + "/documents";
    }

    @GetMapping("/documents/{id}/file")
    public ResponseEntity<byte[]> file(
            @AuthenticationPrincipal PortalPrincipal actor, @PathVariable long id)
            throws IOException {
        var path = documents.authorizedFile(actor, id);
        return ResponseEntity.ok()
                .contentType(MediaType.IMAGE_PNG)
                .header("Cache-Control", "no-store")
                .header("Content-Disposition", "inline; filename=document.png")
                .body(Files.readAllBytes(path));
    }

    @GetMapping("/applications/{id}/ocr")
    public String ocr(
            @AuthenticationPrincipal PortalPrincipal actor, @PathVariable long id, Model model)
            throws IOException {
        var application = workflow.view(actor, id);
        model.addAttribute("application", application);
        model.addAttribute("liveEnabled", settings.liveEnabled());
        model.addAttribute("aiEnabled", settings.aiEnabled());
        var certificate =
                mapper.current(id).stream()
                        .filter(document -> "BUSINESS_CERT".equals(document.getTypeCode()))
                        .findFirst()
                        .orElse(null);
        model.addAttribute("certificate", certificate);
        var extracted = new HashMap<String, String>();
        var confirmed = new HashMap<String, String>();
        if (certificate != null) {
            model.addAttribute("job", jobs.findDocumentJob(certificate.getId()));
            var result = jobs.resultFor(certificate.getId());
            if (result != null) {
                if (result.get("raw_result") != null) {
                    model.addAttribute(
                            "recognition",
                            new ObjectMapper()
                                    .readValue(result.get("raw_result").toString(), Map.class));
                }
                if (result.get("evidence") != null) {
                    model.addAttribute(
                            "evidence",
                            new ObjectMapper()
                                    .readValue(result.get("evidence").toString(), Map.class));
                }
                extracted.putAll(
                        new ObjectMapper()
                                .readValue(
                                        result.get("extracted_fields").toString(),
                                        new TypeReference<Map<String, String>>() {}));
            }
            jobs.confirmations(certificate.getId())
                    .forEach(
                            row ->
                                    confirmed.put(
                                            row.get("field_code").toString(),
                                            row.get("confirmed_value").toString()));
        }
        model.addAttribute("extracted", extracted);
        model.addAttribute("confirmed", confirmed);
        return "ocr";
    }

    @PostMapping("/documents/{id}/ocr")
    public String requestOcr(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam(defaultValue = "SAMPLE") String mode) {
        ocr.request(actor, id, mode);
        return "redirect:/applications/" + mapper.find(id).getApplicationId() + "/ocr";
    }

    @PostMapping("/documents/{id}/apply-confirmed")
    public String applyConfirmed(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam long version,
            RedirectAttributes flash) {
        ocr.applyConfirmed(actor, id, version);
        flash.addFlashAttribute("notice", "확인한 사업자 정보를 신청서에 반영했습니다. 사업장·계획·사업비를 함께 확인해 주세요.");
        return "redirect:/applications/" + mapper.find(id).getApplicationId() + "/edit";
    }

    @PostMapping("/documents/{id}/confirm")
    public String confirm(
            @AuthenticationPrincipal PortalPrincipal actor,
            @PathVariable long id,
            @RequestParam Map<String, String> fields,
            RedirectAttributes flash) {
        ocr.confirm(actor, id, fields);
        flash.addFlashAttribute("notice", "원래 추출값과 별도로 확인값을 저장했습니다.");
        return "redirect:/applications/" + mapper.find(id).getApplicationId() + "/ocr";
    }
}
