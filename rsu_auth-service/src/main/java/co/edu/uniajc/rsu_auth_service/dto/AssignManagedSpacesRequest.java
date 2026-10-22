package co.edu.uniajc.rsu_auth_service.dto;

import java.util.Set;
import java.util.UUID;

import jakarta.validation.constraints.NotNull;

public record AssignManagedSpacesRequest(@NotNull Set<UUID> spaceIds) {
}
