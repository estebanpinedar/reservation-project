package co.edu.uniajc.rsu_auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record SaveSpaceTypeRequest(
        @NotBlank @Size(max = 80) String name,
        @Size(max = 250) String description) {
}
