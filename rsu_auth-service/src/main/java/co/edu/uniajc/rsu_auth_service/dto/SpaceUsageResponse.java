package co.edu.uniajc.rsu_auth_service.dto;

import java.math.BigDecimal;

public record SpaceUsageResponse(Long spaceId, String spaceName, long reservationCount, BigDecimal occupiedHours) {
}
