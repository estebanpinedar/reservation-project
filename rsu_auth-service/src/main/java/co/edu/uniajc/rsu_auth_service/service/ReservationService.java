package co.edu.uniajc.rsu_auth_service.service;

import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniajc.rsu_auth_service.exeption.BadRequestException;
import co.edu.uniajc.rsu_auth_service.exeption.ConflictException;
import co.edu.uniajc.rsu_auth_service.exeption.NotFoundException;
import co.edu.uniajc.rsu_auth_service.dto.CancelReservationRequest;
import co.edu.uniajc.rsu_auth_service.dto.CreateReservationRequest;
import co.edu.uniajc.rsu_auth_service.dto.ReservationResponse;
import co.edu.uniajc.rsu_auth_service.dto.ReservationOverviewResponse;
import co.edu.uniajc.rsu_auth_service.model.Reservation;
import co.edu.uniajc.rsu_auth_service.model.ReservationStatus;
import co.edu.uniajc.rsu_auth_service.model.BlockStatus;
import co.edu.uniajc.rsu_auth_service.repository.ReservationRepository;
import co.edu.uniajc.rsu_auth_service.repository.SpaceBlockRepository;
import co.edu.uniajc.rsu_auth_service.model.Space;
import co.edu.uniajc.rsu_auth_service.model.SpaceStatus;
import co.edu.uniajc.rsu_auth_service.repository.SpaceRepository;
import co.edu.uniajc.rsu_auth_service.model.User;
import co.edu.uniajc.rsu_auth_service.repository.UserRepository;

@Service
public class ReservationService {
    private final ReservationRepository reservationRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final SpaceBlockRepository spaceBlockRepository;
    private final AuditService auditService;

    public ReservationService(ReservationRepository reservationRepository, SpaceRepository spaceRepository,
            UserRepository userRepository, SpaceBlockRepository spaceBlockRepository, AuditService auditService) {
        this.reservationRepository = reservationRepository;
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
        this.spaceBlockRepository = spaceBlockRepository;
        this.auditService = auditService;
    }

    @Transactional
    public ReservationResponse create(Long creatorId, CreateReservationRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        validateSameLocalDay(request.startAt(), request.endAt());
        validateHalfHourTimes(request.startAt(), request.endAt());
        Space space = spaceRepository.lockByIdAndStatus(request.spaceId(), SpaceStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Active space was not found"));
        if (!space.isAvailableDuring(request.startAt(), request.endAt(), ZoneId.of("America/Bogota"))) {
            throw new BadRequestException("El horario solicitado está fuera de los días u horas disponibles del espacio.");
        }
        if (request.attendees() > space.getCapacity()) {
            throw new BadRequestException("Attendees exceed the capacity of the selected space");
        }
        if (reservationRepository.existsBySpaceIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(space.getId(),
                ReservationStatus.ACTIVE, request.endAt(), request.startAt())) {
            throw new ConflictException("The selected space is already reserved for this period");
        }
        if (spaceBlockRepository.existsBySpaceIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(space.getId(),
                BlockStatus.ACTIVE, request.endAt(), request.startAt())) {
            throw new ConflictException("The selected space is blocked for this period");
        }
        User creator = userRepository.getReferenceById(creatorId);
        Reservation reservation = reservationRepository.save(new Reservation(creator, space, request.startAt(), request.endAt(),
                request.purpose().trim(), request.attendees()));
        auditService.record(creatorId, "RESERVATION_CREATED", "RESERVATION", reservation.getId(),
                "Space: " + space.getName());
        return toResponse(reservation);
    }

    @Transactional(readOnly = true)
    public List<ReservationResponse> listMine(Long creatorId, ReservationStatus status, OffsetDateTime startAt,
            OffsetDateTime endAt) {
        validateOptionalPeriod(startAt, endAt);
        OffsetDateTime now = OffsetDateTime.now();
        return reservationRepository.findByCreatorIdOrderByStartAtDesc(creatorId).stream()
                .filter(reservation -> status == null || effectiveStatus(reservation, now) == status)
                .filter(reservation -> startAt == null || reservation.getEndAt().isAfter(startAt))
                .filter(reservation -> endAt == null || reservation.getStartAt().isBefore(endAt))
                .map(reservation -> toResponse(reservation, now)).toList();
    }

    @Transactional(readOnly = true)
    public List<ReservationOverviewResponse> listAgenda(OffsetDateTime startAt, OffsetDateTime endAt, Long spaceId,
            ReservationStatus status) {
        validatePeriod(startAt, endAt);
        OffsetDateTime now = OffsetDateTime.now();
        return reservationRepository.findAgenda(startAt, endAt, spaceId, null).stream()
                .filter(reservation -> status == null || effectiveStatus(reservation, now) == status)
                .map(reservation -> new ReservationOverviewResponse(reservation.getId(), reservation.getSpace().getId(),
                        reservation.getSpace().getName(), reservation.getCreator().getName(),
                        reservation.getCreator().getEmail(), reservation.getStartAt(), reservation.getEndAt(),
                        reservation.getPurpose(), reservation.getAttendees(), effectiveStatus(reservation, now).name()))
                .toList();
    }

    @Transactional
    public ReservationResponse cancel(Long reservationId, Long requesterId, boolean requesterIsAdministrator,
            CancelReservationRequest request) {
        Reservation reservation = reservationRepository.findWithDetailsById(reservationId)
                .orElseThrow(() -> new NotFoundException("Reservation was not found"));
        if (!requesterIsAdministrator && !reservation.getCreator().getId().equals(requesterId)) {
            throw new NotFoundException("Reservation was not found");
        }
        if (reservation.getStatus() != ReservationStatus.ACTIVE) {
            throw new ConflictException("Only active reservations can be cancelled");
        }
        if (!reservation.getEndAt().isAfter(OffsetDateTime.now())) {
            throw new ConflictException("Finished reservations cannot be cancelled");
        }
        reservation.cancel(request.reason().trim());
        auditService.record(requesterId, "RESERVATION_CANCELLED", "RESERVATION", reservation.getId(),
                "Space: " + reservation.getSpace().getName());
        return toResponse(reservation);
    }

    private void validatePeriod(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new BadRequestException("Start time must be before end time");
        }
    }

    private void validateHalfHourTimes(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (!isOnHalfHour(startAt) || !isOnHalfHour(endAt)) {
            throw new BadRequestException(
                    "Las horas de inicio y finalización deben estar en intervalos de 30 minutos (00 o 30)");
        }
    }

    private void validateSameLocalDay(OffsetDateTime startAt, OffsetDateTime endAt) {
        var zone = ZoneId.of("America/Bogota");
        if (!startAt.atZoneSameInstant(zone).toLocalDate().equals(endAt.atZoneSameInstant(zone).toLocalDate())) {
            throw new BadRequestException("La reserva debe iniciar y finalizar el mismo día.");
        }
    }

    private boolean isOnHalfHour(OffsetDateTime value) {
        var time = value.atZoneSameInstant(ZoneId.of("America/Bogota")).toLocalTime();
        return (time.getMinute() == 0 || time.getMinute() == 30) && time.getSecond() == 0 && time.getNano() == 0;
    }

    private void validateOptionalPeriod(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
            throw new BadRequestException("Start time must be before end time");
        }
    }

    private ReservationResponse toResponse(Reservation reservation) {
        return toResponse(reservation, OffsetDateTime.now());
    }

    private ReservationResponse toResponse(Reservation reservation, OffsetDateTime now) {
        return new ReservationResponse(reservation.getId(), reservation.getSpace().getId(), reservation.getSpace().getName(),
                reservation.getStartAt(), reservation.getEndAt(), reservation.getPurpose(), reservation.getAttendees(),
                effectiveStatus(reservation, now).name(), reservation.getCancelledAt(), reservation.getCancellationReason(),
                reservation.getCreatedAt());
    }

    private ReservationStatus effectiveStatus(Reservation reservation, OffsetDateTime now) {
        if (reservation.getStatus() == ReservationStatus.ACTIVE && !reservation.getEndAt().isAfter(now)) {
            return ReservationStatus.FINISHED;
        }
        return reservation.getStatus();
    }
}
