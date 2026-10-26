package kr.co.maeum.support.service;

import com.fasterxml.jackson.databind.ObjectMapper;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.dto.ApplicationForm;
import kr.co.maeum.support.mapper.ProgramMapper;
import kr.co.maeum.support.security.PortalPrincipal;
import kr.co.maeum.support.service.policy.SampleDatasetPolicy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.time.LocalDate;

/** 예시 입력은 사용자가 요청할 때만 수행하며 서류 첨부·OCR을 자동 실행하지 않는다. */
@Service
public class SampleApplicationService {
    private final ApplicationService workflow;

    private final ProgramMapper programs;

    public SampleApplicationService(ApplicationService workflow, ProgramMapper programs) {
        this.workflow = workflow;
        this.programs = programs;
    }

    @Transactional
    public void apply(PortalPrincipal actor, long id, long version, String dataset) {
        var application = workflow.view(actor, id);
        var program = programs.find(application.getProgramId());
        if (SampleDatasetPolicy.choices(program).stream()
                .noneMatch(option -> option.code().equals(dataset))) {
            throw new BusinessException(422, "해당 사업의 신청 예시를 선택해 주세요.");
        }
        try (var stream =
                getClass().getResourceAsStream("/fixtures/" + dataset + "/application.json")) {
            if (stream == null) {
                throw new IllegalStateException("예시 신청 정보가 없습니다.");
            }
            var sample = new ObjectMapper().readTree(stream).path("application");
            var form = new ApplicationForm();
            form.setVersion(version);
            form.setAddress(sample.path("address").asText());
            form.setBusinessType(sample.path("businessType").asText());
            form.setBusinessItem(sample.path("businessItem").asText());
            form.setSelfContribution(sample.path("selfContribution").decimalValue());
            form.setBusinessName(sample.path("businessName").asText());
            form.setBusinessNo(sample.path("businessNo").asText());
            form.setRepresentative(sample.path("representative").asText());
            form.setOpenedOn(LocalDate.parse(sample.path("openedOn").asText()));
            form.setPhone(sample.path("phone").asText());
            form.setRequestedAmount(sample.path("requestedAmount").decimalValue());
            form.setPlanText(sample.path("planText").asText());
            workflow.save(actor, id, form);
        } catch (IOException exception) {
            throw new IllegalStateException("예시 신청 정보를 읽지 못했습니다.", exception);
        }
    }
}
