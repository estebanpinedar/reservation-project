package co.edu.uniajc.rsu_auth_service.dto;

import java.time.DayOfWeek;
import java.time.LocalTime;

public record SpaceScheduleResponse(DayOfWeek dayOfWeek, LocalTime opensAt, LocalTime closesAt) {
}
