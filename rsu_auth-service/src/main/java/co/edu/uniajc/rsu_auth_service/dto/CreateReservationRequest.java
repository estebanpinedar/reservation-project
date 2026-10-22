package co.edu.uniajc.rsu_auth_service.dto;

import java.time.OffsetDateTime;
import java.util.UUID;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public record CreateReservationRequest(
        @NotNull(message = "Space is required") UUID spaceId,
        @NotNull(message = "Start time is required") @Future(message = "Start time must be in the future") OffsetDateTime startAt,
        @NotNull(message = "End time is required") @Future(message = "End time must be in the future") OffsetDateTime endAt,
        @NotBlank(message = "Purpose is required") @Size(max = 250, message = "Purpose must not exceed 250 characters") String purpose,
        @NotNull(message = "Attendees is required") @Min(value = 1, message = "At least one attendee is required") Integer attendees) {
}
