package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;

public record SpaceBlockResponse(Long id, Long spaceId, String spaceName, OffsetDateTime startAt, OffsetDateTime endAt,
        String reason, String status, OffsetDateTime cancelledAt, OffsetDateTime createdAt) {
}
