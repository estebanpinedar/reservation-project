package co.edu.uniajc.rsu_auth_service.dto;

import java.util.Set;

import co.edu.uniajc.rsu_auth_service.model.RoleName;
import jakarta.validation.constraints.NotEmpty;

public record UpdateUserRolesRequest(@NotEmpty Set<RoleName> roles) {
}
