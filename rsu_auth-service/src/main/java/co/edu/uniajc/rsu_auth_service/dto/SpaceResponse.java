package co.edu.uniajc.rsu_auth_service.dto;

import java.util.List;

public record SpaceResponse(Long id, String name, String location, int capacity, String description, String status,
        SpaceTypeResponse type, List<SpaceScheduleResponse> schedules) {
}
