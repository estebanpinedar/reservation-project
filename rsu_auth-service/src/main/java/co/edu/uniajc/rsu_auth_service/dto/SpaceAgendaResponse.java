package co.edu.uniajc.rsu_auth_service.dto;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;

public record SpaceAgendaResponse(UUID spaceId, String spaceName, LocalDate date,
        List<AgendaEntryResponse> entries) {
}
