package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;

public record ReservationResponse(Long id, Long spaceId, String spaceName, OffsetDateTime startAt, OffsetDateTime endAt,
        String purpose, int attendees, String status, OffsetDateTime cancelledAt, String cancellationReason,
        OffsetDateTime createdAt) {
}
