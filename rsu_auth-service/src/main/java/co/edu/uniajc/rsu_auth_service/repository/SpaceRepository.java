package co.edu.uniajc.rsu_auth_service.repository;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import co.edu.uniajc.rsu_auth_service.model.Space;
import co.edu.uniajc.rsu_auth_service.model.SpaceStatus;

public interface SpaceRepository extends JpaRepository<Space, UUID> {
    boolean existsByNameIgnoreCase(String name);

    @EntityGraph(attributePaths = "spaceType")
    List<Space> findAllByOrderByNameAsc();

    @Query("""
            select space from Space space join fetch space.spaceType
            where (:status is null or space.status = :status)
              and (:query is null or lower(space.name) like lower(concat('%', :query, '%'))
                   or lower(space.location) like lower(concat('%', :query, '%')))
              and (:type is null or lower(space.spaceType.name) = lower(:type))
              and (:minimumCapacity is null or space.capacity >= :minimumCapacity)
            order by space.name asc
            """)
    List<Space> searchForManagement(@Param("status") SpaceStatus status, @Param("query") String query,
            @Param("type") String type, @Param("minimumCapacity") Integer minimumCapacity);

    @Query("""
            select space from Space space join fetch space.spaceType
            where space.status = :status
              and (:query is null or lower(space.name) like lower(concat('%', :query, '%'))
                   or lower(space.location) like lower(concat('%', :query, '%')))
              and (:type is null or lower(space.spaceType.name) = lower(:type))
              and (:minimumCapacity is null or space.capacity >= :minimumCapacity)
            order by space.name asc
            """)
    List<Space> searchActive(@Param("status") SpaceStatus status, @Param("query") String query,
            @Param("type") String type, @Param("minimumCapacity") Integer minimumCapacity);

    @Query("""
            select space from Space space join fetch space.spaceType
            where space.status = :status
              and (:minimumCapacity is null or space.capacity >= :minimumCapacity)
              and not exists (
                  select reservation.id from Reservation reservation
                  where reservation.space = space and reservation.status = :reservationStatus
                    and reservation.startAt < :endAt and reservation.endAt > :startAt)
              and not exists (
                  select block.id from SpaceBlock block
                  where block.space = space and block.status = :blockStatus
                    and block.startAt < :endAt and block.endAt > :startAt)
            order by space.name asc
            """)
    List<Space> findAvailable(@Param("status") SpaceStatus status,
            @Param("reservationStatus") co.edu.uniajc.rsu_auth_service.model.ReservationStatus reservationStatus,
            @Param("blockStatus") co.edu.uniajc.rsu_auth_service.model.BlockStatus blockStatus,
            @Param("startAt") java.time.OffsetDateTime startAt, @Param("endAt") java.time.OffsetDateTime endAt,
            @Param("minimumCapacity") Integer minimumCapacity);

    @EntityGraph(attributePaths = "spaceType")
    Optional<Space> findByIdAndStatus(UUID id, SpaceStatus status);

    @EntityGraph(attributePaths = "spaceType")
    Optional<Space> findWithTypeById(UUID id);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select space from Space space join fetch space.spaceType where space.id = :id and space.status = :status")
    Optional<Space> lockByIdAndStatus(@Param("id") UUID id, @Param("status") SpaceStatus status);
}
