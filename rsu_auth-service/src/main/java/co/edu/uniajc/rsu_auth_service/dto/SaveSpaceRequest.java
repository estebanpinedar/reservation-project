package co.edu.uniajc.rsu_auth_service.dto;

import java.util.UUID;

import co.edu.uniajc.rsu_auth_service.model.SpaceStatus;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record SaveSpaceRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull UUID typeId,
        @NotBlank @Size(max = 160) String location,
        @NotNull @Min(1) Integer capacity,
        @Size(max = 500) String description,
        SpaceStatus status) {
}
