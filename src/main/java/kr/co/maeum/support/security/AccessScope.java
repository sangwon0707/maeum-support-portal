package kr.co.maeum.support.security;

import kr.co.maeum.support.common.BusinessException;
import kr.co.maeum.support.mapper.ApplicationMapper;
import kr.co.maeum.support.mapper.IdentityMapper;
import kr.co.maeum.support.model.Application;

import org.springframework.stereotype.Component;

/** 화면의 버튼 유무와 무관하게 모든 업무 진입점에서 같은 권한 규칙을 적용한다. */
@Component
public class AccessScope {
    private final IdentityMapper identities;
    private final ApplicationMapper applications;

    public AccessScope(IdentityMapper identities, ApplicationMapper applications) {
        this.identities = identities;
        this.applications = applications;
    }

    public void active(PortalPrincipal actor) {
        if (actor == null || identities.activeWorkspace(actor.workspaceId()) != 1) {
            throw BusinessException.forbidden();
        }
    }

    public void role(PortalPrincipal actor, String role) {
        active(actor);
        if (!role.equals(actor.role())) {
            throw BusinessException.forbidden();
        }
    }

    public void view(PortalPrincipal actor, Application application) {
        active(actor);
        if (application == null) {
            throw new BusinessException(404, "신청 내역을 찾을 수 없습니다.");
        }
        if (actor.workspaceId() != application.getWorkspaceId()) {
            throw BusinessException.forbidden();
        }
        boolean allowed =
                switch (actor.role()) {
                    case "APPLICANT" -> actor.id() == application.getOwnerId();
                    case "STAFF" -> applications.isAssigned(application.getId(), actor.id()) == 1;
                    case "ADMIN" -> true;
                    default -> false;
                };
        if (!allowed) {
            throw BusinessException.forbidden();
        }
    }

    public void owner(PortalPrincipal actor, Application application) {
        role(actor, "APPLICANT");
        view(actor, application);
    }

    public void reviewer(PortalPrincipal actor, Application application) {
        active(actor);
        // 관리자는 같은 공간의 접수를 직접 검토하고, 담당자는 기존 배정 범위를 유지한다.
        if (!"ADMIN".equals(actor.role()) && !"STAFF".equals(actor.role())) {
            throw BusinessException.forbidden();
        }
        view(actor, application);
    }
}
