package co.edu.uniajc.rsu_auth_service.dto;

import java.util.UUID;

public record SpaceResponse(UUID id, String name, String location, int capacity, String description, String status,
        SpaceTypeResponse type) {
}
