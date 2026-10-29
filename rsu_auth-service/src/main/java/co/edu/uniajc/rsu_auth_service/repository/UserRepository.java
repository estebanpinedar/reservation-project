package co.edu.uniajc.rsu_auth_service.repository;

import java.util.Optional;
import java.util.List;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

import co.edu.uniajc.rsu_auth_service.model.User;
import co.edu.uniajc.rsu_auth_service.model.UserStatus;
import co.edu.uniajc.rsu_auth_service.model.RoleName;

public interface UserRepository extends JpaRepository<User, Long> {
    boolean existsByEmailIgnoreCase(String email);

    boolean existsByIdAndStatus(Long id, UserStatus status);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findByEmailIgnoreCase(String email);

    @EntityGraph(attributePaths = "roles")
    Optional<User> findWithRolesById(Long id);

    @EntityGraph(attributePaths = { "roles", "managedSpaces" })
    Optional<User> findWithRolesAndManagedSpacesById(Long id);

    @EntityGraph(attributePaths = { "roles", "managedSpaces" })
    List<User> findAllByOrderByNameAsc();

    boolean existsByIdAndManagedSpaces_Id(Long userId, Long spaceId);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select distinct user from User user join user.roles role where user.status = :status and role.name = :roleName")
    List<User> lockByStatusAndRole(@Param("status") UserStatus status, @Param("roleName") RoleName roleName);
}
