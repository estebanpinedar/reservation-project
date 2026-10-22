package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AuditEntryResponse(Long id, UUID actorId, String actorName, String action, String entity,
        UUID entityId, String detail, OffsetDateTime createdAt) {
}
