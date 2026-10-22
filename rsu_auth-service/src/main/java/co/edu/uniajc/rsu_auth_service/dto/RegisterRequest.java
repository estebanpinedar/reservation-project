package co.edu.uniajc.rsu_auth_service.dto;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record RegisterRequest(
        @NotBlank(message = "Name is required") @Size(max = 120, message = "Name must not exceed 120 characters") String name,
        @NotBlank(message = "Email is required") @Email(message = "Email must be valid") @Size(max = 255, message = "Email must not exceed 255 characters") String email,
        @NotBlank(message = "Password is required")
        @Size(min = 12, max = 72, message = "Password must contain between 12 and 72 characters")
        @Pattern(regexp = "(?s)(?=.*\\p{Lu})(?=.*\\p{Ll})(?=.*\\p{N})(?=.*[^\\p{L}\\p{N}\\s]).*",
                message = "Password must include uppercase and lowercase letters, a number, and a special character") String password) {
}
