package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReservationRequest(
        @NotNull(message = "Space is required") Long spaceId,
        @NotNull(message = "Start time is required") @Future(message = "La hora de inicio debe ser posterior a la hora actual") OffsetDateTime startAt,
        @NotNull(message = "End time is required") @Future(message = "La hora de finalización debe ser posterior a la hora actual") OffsetDateTime endAt,
        @NotBlank(message = "Purpose is required") @Size(max = 250, message = "Purpose must not exceed 250 characters") String purpose,
        @NotNull(message = "Attendees is required") @Min(value = 1, message = "At least one attendee is required") Integer attendees) {
}
