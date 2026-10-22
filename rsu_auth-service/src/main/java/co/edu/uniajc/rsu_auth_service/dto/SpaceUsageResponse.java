package co.edu.uniajc.rsu_auth_service.dto;

import java.math.BigDecimal;
import java.util.UUID;

public record SpaceUsageResponse(UUID spaceId, String spaceName, long reservationCount, BigDecimal occupiedHours) {
}
