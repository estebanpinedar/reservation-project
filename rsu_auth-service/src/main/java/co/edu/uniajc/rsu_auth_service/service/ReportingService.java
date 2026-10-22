package co.edu.uniajc.rsu_auth_service.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniajc.rsu_auth_service.dto.AuditEntryResponse;
import co.edu.uniajc.rsu_auth_service.dto.SpaceUsageResponse;
import co.edu.uniajc.rsu_auth_service.exeption.BadRequestException;
import co.edu.uniajc.rsu_auth_service.model.AuditEntry;
import co.edu.uniajc.rsu_auth_service.model.Reservation;
import co.edu.uniajc.rsu_auth_service.model.ReservationStatus;
import co.edu.uniajc.rsu_auth_service.model.Space;
import co.edu.uniajc.rsu_auth_service.repository.AuditEntryRepository;
import co.edu.uniajc.rsu_auth_service.repository.ReservationRepository;
import co.edu.uniajc.rsu_auth_service.repository.SpaceRepository;

@Service
public class ReportingService {
    private final SpaceRepository spaceRepository;
    private final ReservationRepository reservationRepository;
    private final AuditEntryRepository auditEntryRepository;

    public ReportingService(SpaceRepository spaceRepository, ReservationRepository reservationRepository,
            AuditEntryRepository auditEntryRepository) {
        this.spaceRepository = spaceRepository;
        this.reservationRepository = reservationRepository;
        this.auditEntryRepository = auditEntryRepository;
    }

    @Transactional(readOnly = true)
    public List<SpaceUsageResponse> usage(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new BadRequestException("Start time must be before end time");
        }
        Map<UUID, UsageAccumulator> usage = new LinkedHashMap<>();
        for (Space space : spaceRepository.findAllByOrderByNameAsc()) {
            usage.put(space.getId(), new UsageAccumulator(space.getName()));
        }
        List<Reservation> reservations = reservationRepository.findAgenda(startAt, endAt, null, null);
        for (Reservation reservation : reservations) {
            if (reservation.getStatus() == ReservationStatus.CANCELLED) {
                continue;
            }
            OffsetDateTime occupiedStart = reservation.getStartAt().isBefore(startAt) ? startAt : reservation.getStartAt();
            OffsetDateTime occupiedEnd = reservation.getEndAt().isAfter(endAt) ? endAt : reservation.getEndAt();
            long seconds = Duration.between(occupiedStart, occupiedEnd).getSeconds();
            UsageAccumulator accumulator = usage.get(reservation.getSpace().getId());
            accumulator.reservations++;
            accumulator.occupiedSeconds += Math.max(0, seconds);
        }
        return usage.entrySet().stream().map(entry -> new SpaceUsageResponse(entry.getKey(), entry.getValue().name,
                entry.getValue().reservations,
                BigDecimal.valueOf(entry.getValue().occupiedSeconds).divide(BigDecimal.valueOf(3600), 2,
                        RoundingMode.HALF_UP)))
                .sorted(Comparator.comparing(SpaceUsageResponse::spaceName)).toList();
    }

    @Transactional(readOnly = true)
    public List<AuditEntryResponse> recentAudit() {
        return auditEntryRepository.findTop100ByOrderByCreatedAtDesc().stream().map(this::toAuditResponse).toList();
    }

    private AuditEntryResponse toAuditResponse(AuditEntry entry) {
        return new AuditEntryResponse(entry.getId(), entry.getActor() == null ? null : entry.getActor().getId(),
                entry.getActor() == null ? "Sistema" : entry.getActor().getName(), entry.getAction(), entry.getEntity(),
                entry.getEntityId(), entry.getDetail(), entry.getCreatedAt());
    }

    private static final class UsageAccumulator {
        private final String name;
        private long reservations;
        private long occupiedSeconds;

        private UsageAccumulator(String name) {
            this.name = name;
        }
    }
}
