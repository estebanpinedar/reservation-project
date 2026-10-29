package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;

public record AgendaEntryResponse(Long id, String kind, String label, OffsetDateTime startAt,
        OffsetDateTime endAt, String status) {
}
