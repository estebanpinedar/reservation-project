package co.edu.uniajc.rsu_auth_service.dto;

import co.edu.uniajc.rsu_auth_service.model.SpaceStatus;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.util.List;

public record SaveSpaceRequest(
        @NotBlank @Size(max = 120) String name,
        @NotNull Long typeId,
        @NotBlank @Size(max = 160) String location,
        @NotNull @Min(1) Integer capacity,
        @Size(max = 500) String description,
        SpaceStatus status,
        @NotEmpty List<@NotNull @Valid SpaceScheduleRequest> schedules) {
}
