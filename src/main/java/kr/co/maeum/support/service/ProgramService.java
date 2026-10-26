package kr.co.maeum.support.service;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.mapper.ProgramMapper;
import kr.co.maeum.support.model.Program;
import kr.co.maeum.support.security.AccessScope;
import kr.co.maeum.support.security.PortalPrincipal;
import kr.co.maeum.support.service.policy.ApplicationPolicy;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;

@Service
public class ProgramService {
    private final ProgramMapper programs;
    private final AccessScope access;
    private final ApplicationPolicy policy;
    private final AuditService audit;
    private final Clock clock;

    public ProgramService(
            ProgramMapper programs,
            AccessScope access,
            ApplicationPolicy policy,
            AuditService audit,
            Clock clock) {
        this.programs = programs;
        this.access = access;
        this.policy = policy;
        this.audit = audit;
        this.clock = clock;
    }

    @Transactional
    public void save(PortalPrincipal actor, Program form) {
        access.role(actor, "ADMIN");
        policy.text(form.getTitle(), "공고 제목", 200, true);
        policy.text(form.getBody(), "공고 내용", 10000, true);
        policy.text(form.getTargetText(), "지원 대상", 5000, false);
        policy.text(form.getSupportText(), "지원 항목", 5000, false);
        policy.text(form.getExclusionText(), "제외 사항", 5000, false);
        policy.text(form.getSelectionText(), "검토 기준", 5000, false);
        policy.text(form.getContactText(), "문의처", 500, false);
        if (form.getOpensAt() == null
                || form.getClosesAt() == null
                || !form.getOpensAt().isBefore(form.getClosesAt())
                || form.getMaxAmount() == null
                || form.getMaxAmount().signum() <= 0
                || form.getMaxAmount().compareTo(new BigDecimal("10000000")) > 0
                || form.getMaxAmount().scale() > 0) {
            throw new BusinessException(422, "기간과 지원 한도(최대 천만원)를 확인해 주세요.");
        }
        if (!List.of("DRAFT", "PUBLISHED", "CLOSED").contains(form.getStatus())) {
            throw new BusinessException(422, "공고 상태를 확인해 주세요.");
        }
        if (form.getId() != null) {
            var previous = programs.find(form.getId());
            if (previous == null) {
                throw new BusinessException(404, "공고가 없습니다.");
            }
            boolean started = !LocalDateTime.now(clock).isBefore(previous.getOpensAt());
            if (started
                    && (!previous.getOpensAt().equals(form.getOpensAt())
                            || !previous.getClosesAt().equals(form.getClosesAt())
                            || previous.getMaxAmount().compareTo(form.getMaxAmount()) != 0)) {
                throw new BusinessException(422, "접수 시작 후 기간과 지원 한도는 변경할 수 없습니다.");
            }
            if (programs.update(form) != 1) {
                throw BusinessException.conflict();
            }
        } else {
            programs.insert(form);
            programs.insertRule(form.getId(), "BUSINESS_CERT");
            programs.insertRule(form.getId(), "ESTIMATE");
        }
        audit.record(
                actor,
                null,
                "PROGRAM_SAVED",
                "PROGRAM",
                form.getId(),
                "공고 저장: " + form.getStatus());
    }
}
