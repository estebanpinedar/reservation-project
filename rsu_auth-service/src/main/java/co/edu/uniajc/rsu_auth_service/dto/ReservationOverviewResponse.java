package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record ReservationOverviewResponse(UUID id, UUID spaceId, String spaceName, String requesterName,
        String requesterEmail, OffsetDateTime startAt, OffsetDateTime endAt, String purpose, int attendees,
        String status) {
}
