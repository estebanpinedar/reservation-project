package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationResponse(UUID id, UUID spaceId, String spaceName, OffsetDateTime startAt, OffsetDateTime endAt,
        String purpose, int attendees, String status, OffsetDateTime cancelledAt, String cancellationReason,
        OffsetDateTime createdAt) {
}
