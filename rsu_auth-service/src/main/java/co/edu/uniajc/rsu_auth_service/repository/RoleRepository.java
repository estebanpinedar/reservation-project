package co.edu.uniajc.rsu_auth_service.repository;

import java.util.Optional;
import java.util.Collection;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import co.edu.uniajc.rsu_auth_service.model.Role;
import co.edu.uniajc.rsu_auth_service.model.RoleName;

public interface RoleRepository extends JpaRepository<Role, Long> {
    Optional<Role> findByName(RoleName name);

    List<Role> findByNameIn(Collection<RoleName> names);
}
