package co.edu.uniajc.rsu_auth_service.service;

import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.EnumSet;
import java.util.Objects;
import java.util.Set;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniajc.rsu_auth_service.dto.AgendaEntryResponse;
import co.edu.uniajc.rsu_auth_service.dto.SaveSpaceRequest;
import co.edu.uniajc.rsu_auth_service.dto.SaveSpaceTypeRequest;
import co.edu.uniajc.rsu_auth_service.dto.SpaceAgendaResponse;
import co.edu.uniajc.rsu_auth_service.dto.SpaceScheduleRequest;
import co.edu.uniajc.rsu_auth_service.dto.SpaceScheduleResponse;
import co.edu.uniajc.rsu_auth_service.exeption.NotFoundException;
import co.edu.uniajc.rsu_auth_service.exeption.BadRequestException;
import co.edu.uniajc.rsu_auth_service.exeption.ConflictException;
import co.edu.uniajc.rsu_auth_service.dto.SpaceResponse;
import co.edu.uniajc.rsu_auth_service.dto.SpaceTypeResponse;
import co.edu.uniajc.rsu_auth_service.model.BlockStatus;
import co.edu.uniajc.rsu_auth_service.model.ReservationStatus;
import co.edu.uniajc.rsu_auth_service.model.Space;
import co.edu.uniajc.rsu_auth_service.model.SpaceStatus;
import co.edu.uniajc.rsu_auth_service.model.SpaceSchedule;
import co.edu.uniajc.rsu_auth_service.model.SpaceType;
import co.edu.uniajc.rsu_auth_service.repository.UserRepository;
import co.edu.uniajc.rsu_auth_service.repository.SpaceRepository;
import co.edu.uniajc.rsu_auth_service.repository.SpaceTypeRepository;
import co.edu.uniajc.rsu_auth_service.repository.ReservationRepository;
import co.edu.uniajc.rsu_auth_service.repository.SpaceBlockRepository;

@Service
public class SpaceCatalogService {
    private final SpaceRepository spaceRepository;
    private final SpaceTypeRepository spaceTypeRepository;
    private final ReservationRepository reservationRepository;
    private final SpaceBlockRepository spaceBlockRepository;
    private final AuditService auditService;
    private final UserRepository userRepository;

    public SpaceCatalogService(SpaceRepository spaceRepository, SpaceTypeRepository spaceTypeRepository,
            ReservationRepository reservationRepository, SpaceBlockRepository spaceBlockRepository,
            AuditService auditService, UserRepository userRepository) {
        this.spaceRepository = spaceRepository;
        this.spaceTypeRepository = spaceTypeRepository;
        this.reservationRepository = reservationRepository;
        this.spaceBlockRepository = spaceBlockRepository;
        this.auditService = auditService;
        this.userRepository = userRepository;
    }

    @Transactional(readOnly = true)
    public List<SpaceTypeResponse> listTypes() {
        return spaceTypeRepository.findAllByOrderByNameAsc().stream().map(this::toTypeResponse).toList();
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> search(String query, String type, Integer minimumCapacity) {
        return spaceRepository.searchActive(SpaceStatus.ACTIVE, normalize(query), normalize(type), minimumCapacity)
                .stream().map(this::toSpaceResponse).toList();
    }

    @Transactional(readOnly = true)
    public SpaceResponse findActiveById(Long id) {
        return spaceRepository.findByIdAndStatus(id, SpaceStatus.ACTIVE)
                .map(this::toSpaceResponse)
                .orElseThrow(() -> new NotFoundException("Space was not found"));
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> searchForManagement(Long actorId, boolean administrator, String query, String type,
            Integer minimumCapacity, SpaceStatus status) {
        List<Space> spaces = spaceRepository.searchForManagement(status, normalize(query), normalize(type), minimumCapacity);
        if (administrator) {
            return spaces.stream().map(this::toSpaceResponse).toList();
        }
        Set<Long> assignedSpaceIds = userRepository.findWithRolesAndManagedSpacesById(actorId)
                .orElseThrow(() -> new AccessDeniedException("The manager account is unavailable"))
                .getManagedSpaces().stream().map(Space::getId).collect(java.util.stream.Collectors.toSet());
        return spaces.stream().filter(space -> assignedSpaceIds.contains(space.getId()))
                .map(this::toSpaceResponse).toList();
    }

    @Transactional
    public SpaceResponse createSpace(Long actorId, SaveSpaceRequest request) {
        validateSchedules(request.schedules());
        String name = normalizeCatalogText(request.name());
        if (spaceRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A space with this name already exists");
        }
        SpaceType type = spaceTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new NotFoundException("Space type was not found"));
        SpaceStatus status = request.status() == null ? SpaceStatus.ACTIVE : request.status();
        Space space = new Space(type, name, normalizeCatalogText(request.location()), request.capacity(),
                normalize(request.description()), status);
        space.replaceSchedules(toSchedules(space, request.schedules()));
        space = spaceRepository.save(space);
        auditService.record(actorId, "SPACE_CREATED", "SPACE", space.getId(), "Space: " + space.getName());
        return toSpaceResponse(space);
    }

    @Transactional
    public SpaceResponse updateSpace(Long actorId, Long spaceId, SaveSpaceRequest request) {
        validateSchedules(request.schedules());
        Space space = spaceRepository.findWithTypeById(spaceId)
                .orElseThrow(() -> new NotFoundException("Space was not found"));
        String name = normalizeCatalogText(request.name());
        if (!space.getName().equalsIgnoreCase(name) && spaceRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A space with this name already exists");
        }
        SpaceType type = spaceTypeRepository.findById(request.typeId())
                .orElseThrow(() -> new NotFoundException("Space type was not found"));
        SpaceStatus status = request.status() == null ? space.getStatus() : request.status();
        space.update(type, name, normalizeCatalogText(request.location()), request.capacity(), normalize(request.description()), status);
        space.replaceSchedules(toSchedules(space, request.schedules()));
        auditService.record(actorId, "SPACE_UPDATED", "SPACE", space.getId(), "Space: " + space.getName());
        return toSpaceResponse(space);
    }

    @Transactional
    public SpaceResponse changeStatus(Long actorId, boolean administrator, Long spaceId, SpaceStatus status) {
        Space space = spaceRepository.findWithTypeById(spaceId)
                .orElseThrow(() -> new NotFoundException("Space was not found"));
        if (!administrator && !userRepository.existsByIdAndManagedSpaces_Id(actorId, spaceId)) {
            throw new AccessDeniedException("The manager is not assigned to this space");
        }
        if (space.getStatus() != status) {
            String previousStatus = space.getStatus().name();
            space.changeStatus(status);
            auditService.record(actorId, "SPACE_STATUS_CHANGED", "SPACE", space.getId(),
                    previousStatus + " -> " + status.name());
        }
        return toSpaceResponse(space);
    }

    @Transactional
    public SpaceTypeResponse createType(Long actorId, SaveSpaceTypeRequest request) {
        String name = normalizeCatalogText(request.name());
        if (spaceTypeRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A space type with this name already exists");
        }
        SpaceType type = spaceTypeRepository.save(new SpaceType(name, normalize(request.description())));
        auditService.record(actorId, "SPACE_TYPE_CREATED", "SPACE_TYPE", null,
                "Type ID: " + type.getId() + "; Type: " + type.getName());
        return toTypeResponse(type);
    }

    @Transactional
    public SpaceTypeResponse updateType(Long actorId, Long typeId, SaveSpaceTypeRequest request) {
        SpaceType type = spaceTypeRepository.findById(typeId)
                .orElseThrow(() -> new NotFoundException("Space type was not found"));
        String name = normalizeCatalogText(request.name());
        if (!type.getName().equalsIgnoreCase(name) && spaceTypeRepository.existsByNameIgnoreCase(name)) {
            throw new ConflictException("A space type with this name already exists");
        }
        type.update(name, normalize(request.description()));
        auditService.record(actorId, "SPACE_TYPE_UPDATED", "SPACE_TYPE", null,
                "Type ID: " + type.getId() + "; Type: " + type.getName());
        return toTypeResponse(type);
    }

    @Transactional(readOnly = true)
    public List<SpaceResponse> findAvailable(OffsetDateTime startAt, OffsetDateTime endAt, Integer minimumCapacity) {
        validatePeriod(startAt, endAt);
        return spaceRepository.findAvailable(SpaceStatus.ACTIVE, ReservationStatus.ACTIVE, BlockStatus.ACTIVE,
                startAt, endAt, minimumCapacity).stream()
                .filter(space -> space.isAvailableDuring(startAt, endAt, ZoneId.of("America/Bogota")))
                .map(this::toSpaceResponse).toList();
    }

    @Transactional(readOnly = true)
    public SpaceAgendaResponse agenda(Long spaceId, LocalDate date) {
        Space space = spaceRepository.findByIdAndStatus(spaceId, SpaceStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Space was not found"));
        ZoneId zone = ZoneId.of("America/Bogota");
        OffsetDateTime startAt = date.atStartOfDay(zone).toOffsetDateTime();
        OffsetDateTime endAt = date.plusDays(1).atStartOfDay(zone).toOffsetDateTime();
        SpaceSchedule schedule = space.scheduleFor(date.getDayOfWeek());
        List<AgendaEntryResponse> entries = new ArrayList<>();
        reservationRepository.findBySpaceIdAndStatusAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
                spaceId, ReservationStatus.ACTIVE, endAt, startAt).forEach(reservation -> entries.add(
                        new AgendaEntryResponse(reservation.getId(), "RESERVATION", "Reservado",
                                reservation.getStartAt(), reservation.getEndAt(), reservation.getStatus().name())));
        spaceBlockRepository.findBySpaceIdAndStatusAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(
                spaceId, BlockStatus.ACTIVE, endAt, startAt).forEach(block -> entries.add(
                        new AgendaEntryResponse(block.getId(), "BLOCK", "Bloqueado",
                                block.getStartAt(), block.getEndAt(), block.getStatus().name())));
        entries.sort(Comparator.comparing(AgendaEntryResponse::startAt));
        return new SpaceAgendaResponse(spaceId, space.getName(), date,
                schedule == null ? null : schedule.getOpensAt(), schedule == null ? null : schedule.getClosesAt(),
                List.copyOf(entries));
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private String normalizeCatalogText(String value) {
        return CatalogTitleNormalizer.normalize(value);
    }

    private void validatePeriod(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new BadRequestException("Start time must be before end time");
        }
    }

    private SpaceResponse toSpaceResponse(Space space) {
        return new SpaceResponse(space.getId(), space.getName(), space.getLocation(), space.getCapacity(),
                space.getDescription(), space.getStatus().name(), toTypeResponse(space.getSpaceType()),
                space.getSchedules().stream().sorted(Comparator.comparing(SpaceSchedule::getDayOfWeek))
                        .map(schedule -> new SpaceScheduleResponse(schedule.getDayOfWeek(), schedule.getOpensAt(),
                                schedule.getClosesAt()))
                        .toList());
    }

    private void validateSchedules(List<SpaceScheduleRequest> schedules) {
        if (schedules == null || schedules.isEmpty()) {
            throw new BadRequestException("Configura al menos un día y horario disponible para el espacio.");
        }
        Set<java.time.DayOfWeek> days = EnumSet.noneOf(java.time.DayOfWeek.class);
        for (SpaceScheduleRequest schedule : schedules) {
            if (schedule == null || schedule.dayOfWeek() == null || schedule.opensAt() == null
                    || schedule.closesAt() == null) {
                throw new BadRequestException("Cada horario debe incluir día, hora de apertura y hora de cierre.");
            }
            if (!days.add(schedule.dayOfWeek())) {
                throw new BadRequestException("Solo se permite un horario por día para cada espacio.");
            }
            if (!schedule.opensAt().isBefore(schedule.closesAt())) {
                throw new BadRequestException("La hora de cierre debe ser posterior a la hora de apertura.");
            }
            boolean validClose = isHalfHour(schedule.closesAt())
                    || schedule.closesAt().equals(java.time.LocalTime.of(23, 59));
            if (!isHalfHour(schedule.opensAt()) || !validClose
                    || java.time.Duration.between(schedule.opensAt(), schedule.closesAt()).toMinutes() < 30) {
                throw new BadRequestException(
                        "Los horarios deben usar intervalos de 30 minutos (00 o 30); el cierre también puede ser 23:59.");
            }
        }
    }

    private boolean isHalfHour(java.time.LocalTime time) {
        return (time.getMinute() == 0 || time.getMinute() == 30) && time.getSecond() == 0 && time.getNano() == 0;
    }

    private List<SpaceSchedule> toSchedules(Space space, List<SpaceScheduleRequest> schedules) {
        return schedules.stream().filter(Objects::nonNull)
                .map(schedule -> new SpaceSchedule(space, schedule.dayOfWeek(), schedule.opensAt(), schedule.closesAt()))
                .toList();
    }

    private SpaceTypeResponse toTypeResponse(SpaceType type) {
        return new SpaceTypeResponse(type.getId(), type.getName(), type.getDescription());
    }
}
