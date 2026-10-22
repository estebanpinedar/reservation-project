package co.edu.uniajc.rsu_auth_service.dto;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, UUID userId, String name,
        String email, List<String> roles) {
}
