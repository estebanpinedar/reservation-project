package co.edu.uniajc.rsu_auth_service.dto;

import java.util.List;
import java.util.UUID;

public record AdminUserResponse(UUID id, String name, String email, String status, List<String> roles,
        List<UUID> managedSpaceIds) {
}
