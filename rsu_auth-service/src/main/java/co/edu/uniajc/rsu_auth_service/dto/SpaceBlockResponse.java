package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

public record SpaceBlockResponse(UUID id, UUID spaceId, String spaceName, OffsetDateTime startAt, OffsetDateTime endAt,
        String reason, String status, OffsetDateTime cancelledAt, OffsetDateTime createdAt) {
}
