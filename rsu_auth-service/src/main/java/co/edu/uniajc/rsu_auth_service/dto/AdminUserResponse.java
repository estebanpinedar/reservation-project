package co.edu.uniajc.rsu_auth_service.dto;

import java.util.List;

public record AdminUserResponse(Long id, String name, String email, String status, List<String> roles,
        List<Long> managedSpaceIds) {
}
