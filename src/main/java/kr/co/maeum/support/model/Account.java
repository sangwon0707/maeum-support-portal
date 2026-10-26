package kr.co.maeum.support.model;

import kr.co.maeum.support.security.PortalPrincipal;

public record Account(
        long id,
        long workspaceId,
        String loginId,
        String passwordHash,
        String displayName,
        String roleCode,
        String workspaceKind) {
    public PortalPrincipal principal() {
        return new PortalPrincipal(
                id,
                workspaceId,
                loginId,
                passwordHash,
                displayName,
                roleCode,
                "DEMO".equals(workspaceKind));
    }
}
