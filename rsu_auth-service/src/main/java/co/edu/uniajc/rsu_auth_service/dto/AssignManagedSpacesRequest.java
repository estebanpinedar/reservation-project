package co.edu.uniajc.rsu_auth_service.dto;

import java.util.Set;

import jakarta.validation.constraints.NotNull;

public record AssignManagedSpacesRequest(@NotNull Set<Long> spaceIds) {
}
