package co.edu.uniajc.rsu_auth_service.dto;

import co.edu.uniajc.rsu_auth_service.model.SpaceStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateSpaceStatusRequest(@NotNull SpaceStatus status) {
}
