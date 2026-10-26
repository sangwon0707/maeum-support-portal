package kr.co.maeum.support.audit;

import kr.co.maeum.support.identity.PortalPrincipal;

import org.springframework.stereotype.Service;

import java.util.HashMap;
import java.util.UUID;

@Service
public class AuditService {
    private final AuditMapper mapper;

    public AuditService(AuditMapper mapper) {
        this.mapper = mapper;
    }

    public void record(
            PortalPrincipal actor,
            Long applicationId,
            String action,
            String targetType,
            long targetId,
            String summary) {
        var event = new HashMap<String, Object>();
        event.put("workspaceId", actor.workspaceId());
        event.put("actorId", actor.id());
        event.put("applicationId", applicationId);
        event.put("action", action);
        event.put("targetType", targetType);
        event.put("targetId", targetId);
        event.put("requestId", UUID.randomUUID().toString());
        event.put("summary", summary);
        mapper.insert(event);
    }
}
