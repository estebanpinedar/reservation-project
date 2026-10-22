package co.edu.uniajc.rsu_auth_service.repository;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.uniajc.rsu_auth_service.model.Reservation;
import co.edu.uniajc.rsu_auth_service.model.ReservationStatus;

public interface ReservationRepository extends JpaRepository<Reservation, UUID> {
    boolean existsBySpaceIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(UUID spaceId, ReservationStatus status,
            OffsetDateTime endAt, OffsetDateTime startAt);

    @EntityGraph(attributePaths = "space")
    List<Reservation> findByCreatorIdOrderByStartAtDesc(UUID creatorId);

    @EntityGraph(attributePaths = { "space", "creator" })
    Optional<Reservation> findWithDetailsById(UUID id);

    @EntityGraph(attributePaths = { "space", "creator" })
    @Query("""
            select reservation from Reservation reservation
            where reservation.startAt < :endAt and reservation.endAt > :startAt
              and (:spaceId is null or reservation.space.id = :spaceId)
              and (:status is null or reservation.status = :status)
            order by reservation.startAt asc
            """)
    List<Reservation> findAgenda(@Param("startAt") OffsetDateTime startAt, @Param("endAt") OffsetDateTime endAt,
            @Param("spaceId") UUID spaceId,
            @Param("status") ReservationStatus status);

    @EntityGraph(attributePaths = "space")
    List<Reservation> findBySpaceIdAndStatusAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(UUID spaceId,
            ReservationStatus status, OffsetDateTime endAt, OffsetDateTime startAt);
}
