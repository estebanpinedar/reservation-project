package co.edu.uniajc.rsu_auth_service.service;

import java.time.OffsetDateTime;
import java.util.UUID;
import java.util.List;
import org.springframework.security.access.AccessDeniedException;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniajc.rsu_auth_service.dto.CreateSpaceBlockRequest;
import co.edu.uniajc.rsu_auth_service.dto.SpaceBlockResponse;
import co.edu.uniajc.rsu_auth_service.exeption.BadRequestException;
import co.edu.uniajc.rsu_auth_service.exeption.ConflictException;
import co.edu.uniajc.rsu_auth_service.exeption.NotFoundException;
import co.edu.uniajc.rsu_auth_service.model.BlockStatus;
import co.edu.uniajc.rsu_auth_service.model.ReservationStatus;
import co.edu.uniajc.rsu_auth_service.model.Space;
import co.edu.uniajc.rsu_auth_service.model.SpaceBlock;
import co.edu.uniajc.rsu_auth_service.model.SpaceStatus;
import co.edu.uniajc.rsu_auth_service.model.User;
import co.edu.uniajc.rsu_auth_service.repository.ReservationRepository;
import co.edu.uniajc.rsu_auth_service.repository.SpaceBlockRepository;
import co.edu.uniajc.rsu_auth_service.repository.SpaceRepository;
import co.edu.uniajc.rsu_auth_service.repository.UserRepository;

@Service
public class SpaceBlockService {
    private final SpaceBlockRepository spaceBlockRepository;
    private final ReservationRepository reservationRepository;
    private final SpaceRepository spaceRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;

    public SpaceBlockService(SpaceBlockRepository spaceBlockRepository, ReservationRepository reservationRepository,
            SpaceRepository spaceRepository, UserRepository userRepository, AuditService auditService) {
        this.spaceBlockRepository = spaceBlockRepository;
        this.reservationRepository = reservationRepository;
        this.spaceRepository = spaceRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
    }

    @Transactional
    public SpaceBlockResponse create(UUID actorId, boolean administrator, CreateSpaceBlockRequest request) {
        validatePeriod(request.startAt(), request.endAt());
        requireSpaceAccess(actorId, administrator, request.spaceId());
        Space space = spaceRepository.lockByIdAndStatus(request.spaceId(), SpaceStatus.ACTIVE)
                .orElseThrow(() -> new NotFoundException("Active space was not found"));
        if (spaceBlockRepository.existsBySpaceIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(space.getId(), BlockStatus.ACTIVE,
                request.endAt(), request.startAt())) {
            throw new ConflictException("The selected space already has a block for this period");
        }
        if (reservationRepository.existsBySpaceIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(space.getId(),
                ReservationStatus.ACTIVE, request.endAt(), request.startAt())) {
            throw new ConflictException("The selected space already has a reservation for this period");
        }
        User actor = userRepository.getReferenceById(actorId);
        SpaceBlock block = spaceBlockRepository.save(new SpaceBlock(space, actor, request.startAt(), request.endAt(),
                request.reason().trim()));
        auditService.record(actorId, "BLOCK_CREATED", "BLOCK", block.getId(), "Space: " + space.getName());
        return toResponse(block);
    }

    @Transactional
    public SpaceBlockResponse cancel(UUID blockId, UUID actorId, boolean administrator) {
        SpaceBlock block = spaceBlockRepository.findWithDetailsById(blockId)
                .orElseThrow(() -> new NotFoundException("Space block was not found"));
        requireSpaceAccess(actorId, administrator, block.getSpace().getId());
        if (block.getStatus() != BlockStatus.ACTIVE) {
            throw new ConflictException("Only active blocks can be cancelled");
        }
        block.cancel();
        auditService.record(actorId, "BLOCK_CANCELLED", "BLOCK", block.getId(), "Space: " + block.getSpace().getName());
        return toResponse(block);
    }

    @Transactional(readOnly = true)
    public List<SpaceBlockResponse> list(UUID actorId, boolean administrator, UUID spaceId,
            OffsetDateTime startAt, OffsetDateTime endAt, BlockStatus status) {
        validateOptionalPeriod(startAt, endAt);
        return spaceBlockRepository.findAgenda(spaceId, startAt, endAt, status).stream()
                .filter(block -> administrator || userRepository.existsByIdAndManagedSpaces_Id(actorId,
                        block.getSpace().getId()))
                .map(this::toResponse).toList();
    }

    private void requireSpaceAccess(UUID actorId, boolean administrator, UUID spaceId) {
        if (!administrator && !userRepository.existsByIdAndManagedSpaces_Id(actorId, spaceId)) {
            throw new AccessDeniedException("The manager is not assigned to this space");
        }
    }

    private void validatePeriod(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt == null || endAt == null || !startAt.isBefore(endAt)) {
            throw new BadRequestException("Start time must be before end time");
        }
    }

    private void validateOptionalPeriod(OffsetDateTime startAt, OffsetDateTime endAt) {
        if (startAt != null && endAt != null && !startAt.isBefore(endAt)) {
            throw new BadRequestException("Start time must be before end time");
        }
    }

    private SpaceBlockResponse toResponse(SpaceBlock block) {
        return new SpaceBlockResponse(block.getId(), block.getSpace().getId(), block.getSpace().getName(), block.getStartAt(),
                block.getEndAt(), block.getReason(), block.getStatus().name(), block.getCancelledAt(), block.getCreatedAt());
    }
}
