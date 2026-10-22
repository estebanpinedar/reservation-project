package co.edu.uniajc.rsu_auth_service.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record CancelReservationRequest(
        @NotBlank(message = "Cancellation reason is required")
        @Size(max = 250, message = "Cancellation reason must not exceed 250 characters") String reason) {
}
