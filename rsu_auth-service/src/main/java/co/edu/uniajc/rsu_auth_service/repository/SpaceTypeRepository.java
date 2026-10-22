package co.edu.uniajc.rsu_auth_service.repository;

import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uniajc.rsu_auth_service.model.SpaceType;

public interface SpaceTypeRepository extends JpaRepository<SpaceType, UUID> {
    List<SpaceType> findAllByOrderByNameAsc();

    boolean existsByNameIgnoreCase(String name);
}
