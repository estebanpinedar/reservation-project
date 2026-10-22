package co.edu.uniajc.rsu_auth_service.dto;

import co.edu.uniajc.rsu_auth_service.model.UserStatus;
import jakarta.validation.constraints.NotNull;

public record UpdateUserStatusRequest(@NotNull UserStatus status) {
}
