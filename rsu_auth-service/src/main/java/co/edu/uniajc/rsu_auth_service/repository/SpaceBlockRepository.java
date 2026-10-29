package co.edu.uniajc.rsu_auth_service.repository;

import java.time.OffsetDateTime;
import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import co.edu.uniajc.rsu_auth_service.model.BlockStatus;
import co.edu.uniajc.rsu_auth_service.model.SpaceBlock;

public interface SpaceBlockRepository extends JpaRepository<SpaceBlock, Long> {
    boolean existsBySpaceIdAndStatusAndStartAtLessThanAndEndAtGreaterThan(Long spaceId, BlockStatus status,
            OffsetDateTime endAt, OffsetDateTime startAt);

    @EntityGraph(attributePaths = { "space", "creator" })
    Optional<SpaceBlock> findWithDetailsById(Long id);

    @EntityGraph(attributePaths = { "space", "creator" })
    @Query("""
            select block from SpaceBlock block
            where (:spaceId is null or block.space.id = :spaceId)
              and (:startAt is null or block.endAt > :startAt)
              and (:endAt is null or block.startAt < :endAt)
              and (:status is null or block.status = :status)
            order by block.startAt asc
            """)
    List<SpaceBlock> findAgenda(@Param("spaceId") Long spaceId, @Param("startAt") java.time.OffsetDateTime startAt,
            @Param("endAt") java.time.OffsetDateTime endAt, @Param("status") BlockStatus status);

    @EntityGraph(attributePaths = { "space", "creator" })
    List<SpaceBlock> findBySpaceIdAndStatusAndStartAtLessThanAndEndAtGreaterThanOrderByStartAtAsc(Long spaceId,
            BlockStatus status, java.time.OffsetDateTime endAt, java.time.OffsetDateTime startAt);
}
