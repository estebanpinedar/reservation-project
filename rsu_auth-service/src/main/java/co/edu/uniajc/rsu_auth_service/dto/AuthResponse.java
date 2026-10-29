package co.edu.uniajc.rsu_auth_service.dto;

import java.time.Instant;
import java.util.List;

public record AuthResponse(String accessToken, String tokenType, Instant expiresAt, Long userId, String name,
        String email, List<String> roles) {
}
