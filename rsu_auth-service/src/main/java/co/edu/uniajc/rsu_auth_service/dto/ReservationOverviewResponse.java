package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;

public record ReservationOverviewResponse(Long id, Long spaceId, String spaceName, String requesterName,
        String requesterEmail, OffsetDateTime startAt, OffsetDateTime endAt, String purpose, int attendees,
        String status) {
}
