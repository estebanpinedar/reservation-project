package co.edu.uniajc.rsu_auth_service.service;

import java.util.List;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniajc.rsu_auth_service.dto.AdminUserResponse;
import co.edu.uniajc.rsu_auth_service.dto.AssignManagedSpacesRequest;
import co.edu.uniajc.rsu_auth_service.dto.UpdateUserRolesRequest;
import co.edu.uniajc.rsu_auth_service.model.UserStatus;
import co.edu.uniajc.rsu_auth_service.exeption.BadRequestException;
import co.edu.uniajc.rsu_auth_service.exeption.NotFoundException;
import co.edu.uniajc.rsu_auth_service.model.Role;
import co.edu.uniajc.rsu_auth_service.model.RoleName;
import co.edu.uniajc.rsu_auth_service.model.Space;
import co.edu.uniajc.rsu_auth_service.model.User;
import co.edu.uniajc.rsu_auth_service.repository.RoleRepository;
import co.edu.uniajc.rsu_auth_service.repository.SpaceRepository;
import co.edu.uniajc.rsu_auth_service.repository.UserRepository;

@Service
public class AdminUserService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final SpaceRepository spaceRepository;
    private final AuditService auditService;

    public AdminUserService(UserRepository userRepository, RoleRepository roleRepository,
            SpaceRepository spaceRepository, AuditService auditService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.spaceRepository = spaceRepository;
        this.auditService = auditService;
    }

    @Transactional(readOnly = true)
    public List<AdminUserResponse> list() {
        return userRepository.findAllByOrderByNameAsc().stream().map(this::toResponse).toList();
    }

    @Transactional
    public AdminUserResponse updateRoles(UUID actorId, UUID userId, UpdateUserRolesRequest request) {
        User user = userRepository.findWithRolesAndManagedSpacesById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found"));
        boolean isAdministrator = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMINISTRATOR);
        if (actorId.equals(userId) && isAdministrator && !request.roles().contains(RoleName.ADMINISTRATOR)) {
            throw new BadRequestException("You cannot remove your own administrator role");
        }
        if (isAdministrator && user.getStatus() == UserStatus.ACTIVE
                && !request.roles().contains(RoleName.ADMINISTRATOR)
                && userRepository.lockByStatusAndRole(UserStatus.ACTIVE, RoleName.ADMINISTRATOR).size() <= 1) {
            throw new BadRequestException("The last active administrator cannot lose the administrator role");
        }
        Set<Role> roles = Set.copyOf(roleRepository.findByNameIn(request.roles()));
        if (roles.size() != request.roles().size()) {
            throw new BadRequestException("One or more requested roles do not exist");
        }
        user.replaceRoles(roles);
        if (roles.stream().noneMatch(role -> role.getName() == RoleName.MANAGER)) {
            user.replaceManagedSpaces(Set.of());
        }
        auditService.record(actorId, "USER_ROLES_UPDATED", "USER", user.getId(),
                roles.stream().map(Role::getName).map(Enum::name).sorted().collect(Collectors.joining(", ")));
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse assignManagedSpaces(UUID actorId, UUID userId, AssignManagedSpacesRequest request) {
        User user = userRepository.findWithRolesAndManagedSpacesById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found"));
        if (!request.spaceIds().isEmpty()
                && user.getRoles().stream().noneMatch(role -> role.getName() == RoleName.MANAGER)) {
            throw new BadRequestException("Only users with the MANAGER role can manage spaces");
        }
        List<Space> spaces = spaceRepository.findAllById(request.spaceIds());
        if (spaces.size() != request.spaceIds().size()) {
            throw new NotFoundException("One or more spaces were not found");
        }
        user.replaceManagedSpaces(Set.copyOf(spaces));
        auditService.record(actorId, "USER_SPACE_ASSIGNMENTS_UPDATED", "USER", user.getId(),
                "Managed spaces: " + spaces.size());
        return toResponse(user);
    }

    @Transactional
    public AdminUserResponse updateStatus(UUID actorId, UUID userId, UserStatus status) {
        if (actorId.equals(userId) && status == UserStatus.INACTIVE) {
            throw new BadRequestException("You cannot deactivate your own account");
        }
        User user = userRepository.findWithRolesAndManagedSpacesById(userId)
                .orElseThrow(() -> new NotFoundException("User was not found"));
        boolean isAdministrator = user.getRoles().stream()
                .anyMatch(role -> role.getName() == RoleName.ADMINISTRATOR);
        if (status == UserStatus.INACTIVE && isAdministrator
                && user.getStatus() == UserStatus.ACTIVE) {
            List<User> activeAdministrators = userRepository.lockByStatusAndRole(UserStatus.ACTIVE,
                    RoleName.ADMINISTRATOR);
            if (activeAdministrators.size() <= 1) {
                throw new BadRequestException("The last active administrator cannot be deactivated");
            }
        }
        if (user.getStatus() != status) {
            String previousStatus = user.getStatus().name();
            user.changeStatus(status);
            auditService.record(actorId, "USER_STATUS_CHANGED", "USER", user.getId(),
                    previousStatus + " -> " + status.name());
        }
        return toResponse(user);
    }

    private AdminUserResponse toResponse(User user) {
        return new AdminUserResponse(user.getId(), user.getName(), user.getEmail(), user.getStatus().name(),
                user.getRoles().stream().map(Role::getName).map(Enum::name).sorted().toList(),
                user.getManagedSpaces().stream().map(Space::getId).sorted().toList());
    }
}
