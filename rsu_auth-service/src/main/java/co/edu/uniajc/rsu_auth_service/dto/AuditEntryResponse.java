package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;

public record AuditEntryResponse(Long id, Long actorId, String actorName, String action, String entity,
        Long entityId, String detail, OffsetDateTime createdAt) {
}
