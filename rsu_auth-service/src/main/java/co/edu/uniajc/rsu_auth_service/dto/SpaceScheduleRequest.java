package co.edu.uniajc.rsu_auth_service.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

import jakarta.validation.constraints.NotNull;

public record SpaceScheduleRequest(
        @NotNull DayOfWeek dayOfWeek,
        @NotNull LocalTime opensAt,
        @NotNull LocalTime closesAt) {
}
