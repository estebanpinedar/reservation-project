package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record AgendaEntryResponse(UUID id, String kind, String label, OffsetDateTime startAt,
        OffsetDateTime endAt, String status) {
}
