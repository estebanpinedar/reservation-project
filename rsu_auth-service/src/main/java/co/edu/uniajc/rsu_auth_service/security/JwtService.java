package co.edu.uniajc.rsu_auth_service.security;

import java.nio.charset.StandardCharsets;
import java.io.IOException;
import java.security.MessageDigest;
import java.time.Instant;
import java.util.Base64;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import org.springframework.stereotype.Service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import co.edu.uniajc.rsu_auth_service.model.Role;
import co.edu.uniajc.rsu_auth_service.model.User;

@Service
public class JwtService {
    private static final Base64.Encoder BASE64_URL_ENCODER = Base64.getUrlEncoder().withoutPadding();
    private static final Base64.Decoder BASE64_URL_DECODER = Base64.getUrlDecoder();
    private static final String HMAC_ALGORITHM = "HmacSHA256";

    private final JwtProperties properties;
    private final ObjectMapper objectMapper;

    public JwtService(JwtProperties properties, ObjectMapper objectMapper) {
        this.properties = properties;
        this.objectMapper = objectMapper;
        if (properties.secret() == null || properties.secret().getBytes(StandardCharsets.UTF_8).length < 32) {
            throw new IllegalStateException("JWT secret must contain at least 32 bytes");
        }
    }

    public Token createToken(User user) {
        Instant issuedAt = Instant.now();
        Instant expiresAt = issuedAt.plus(properties.expiration());
        List<String> roles = user.getRoles().stream().map(Role::getName).map(Enum::name).sorted().toList();

        String header = encodeJson(Map.of("alg", "HS256", "typ", "JWT"));
        String payload = encodeJson(Map.of(
                "sub", user.getId().toString(),
                "email", user.getEmail(),
                "roles", roles,
                "iss", properties.issuer(),
                "iat", issuedAt.getEpochSecond(),
                "exp", expiresAt.getEpochSecond()));
        String signingInput = header + "." + payload;
        return new Token(signingInput + "." + sign(signingInput), expiresAt);
    }

    public AuthenticatedToken parse(String token) {
        String[] parts = token.split("\\.", -1);
        if (parts.length != 3 || parts[0].isBlank() || parts[1].isBlank() || parts[2].isBlank()) {
            throw new InvalidTokenException("Malformed bearer token");
        }

        String signingInput = parts[0] + "." + parts[1];
        try {
            BASE64_URL_DECODER.decode(parts[2]);
        } catch (IllegalArgumentException exception) {
            throw new InvalidTokenException("Malformed bearer token");
        }
        if (!MessageDigest.isEqual(sign(signingInput).getBytes(StandardCharsets.US_ASCII),
                parts[2].getBytes(StandardCharsets.US_ASCII))) {
            throw new InvalidTokenException("Invalid bearer token signature");
        }

        JsonNode payload = readPayload(parts[1]);
        if (!"HS256".equals(readHeader(parts[0]).path("alg").asText())
                || !properties.issuer().equals(payload.path("iss").asText())) {
            throw new InvalidTokenException("Invalid bearer token");
        }
        long expiration = payload.path("exp").asLong(0);
        if (expiration <= Instant.now().getEpochSecond()) {
            throw new InvalidTokenException("Bearer token has expired");
        }
        try {
            UUID userId = UUID.fromString(payload.path("sub").asText());
            List<String> roles = payload.path("roles").isArray()
                    ? toRoleNames(payload.path("roles"))
                    : List.of();
            if (roles.isEmpty()) {
                throw new InvalidTokenException("Bearer token has no roles");
            }
            return new AuthenticatedToken(userId, payload.path("email").asText(), roles);
        } catch (IllegalArgumentException exception) {
            throw new InvalidTokenException("Invalid bearer token subject");
        }
    }

    private List<String> toRoleNames(JsonNode roles) {
        java.util.ArrayList<String> names = new java.util.ArrayList<>();
        roles.forEach(role -> {
            if (!role.isTextual() || role.asText().isBlank()) {
                throw new InvalidTokenException("Invalid bearer token roles");
            }
            names.add(role.asText());
        });
        return List.copyOf(names);
    }

    private JsonNode readHeader(String encodedHeader) {
        return readJson(encodedHeader);
    }

    private JsonNode readPayload(String encodedPayload) {
        return readJson(encodedPayload);
    }

    private JsonNode readJson(String encodedJson) {
        try {
            return objectMapper.readTree(BASE64_URL_DECODER.decode(encodedJson));
        } catch (IllegalArgumentException | IOException exception) {
            throw new InvalidTokenException("Malformed bearer token");
        }
    }

    private String encodeJson(Map<String, ?> value) {
        try {
            return BASE64_URL_ENCODER.encodeToString(objectMapper.writeValueAsBytes(value));
        } catch (JsonProcessingException exception) {
            throw new IllegalStateException("Could not create JWT", exception);
        }
    }

    private String sign(String value) {
        try {
            Mac mac = Mac.getInstance(HMAC_ALGORITHM);
            mac.init(new SecretKeySpec(properties.secret().getBytes(StandardCharsets.UTF_8), HMAC_ALGORITHM));
            return BASE64_URL_ENCODER.encodeToString(mac.doFinal(value.getBytes(StandardCharsets.US_ASCII)));
        } catch (Exception exception) {
            throw new IllegalStateException("Could not sign JWT", exception);
        }
    }

    public record Token(String value, Instant expiresAt) {
    }

    public record AuthenticatedToken(UUID userId, String email, List<String> roles) {
    }
}
