package co.edu.uniajc.rsu_auth_service.dto;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

public record SpaceAgendaResponse(Long spaceId, String spaceName, LocalDate date,
        LocalTime opensAt, LocalTime closesAt, List<AgendaEntryResponse> entries) {
}
