package co.edu.uniajc.rsu_auth_service.controller;

import java.util.List;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniajc.rsu_auth_service.dto.AdminUserResponse;
import co.edu.uniajc.rsu_auth_service.dto.AssignManagedSpacesRequest;
import co.edu.uniajc.rsu_auth_service.dto.UpdateUserRolesRequest;
import co.edu.uniajc.rsu_auth_service.dto.UpdateUserStatusRequest;
import co.edu.uniajc.rsu_auth_service.service.AdminUserService;

@RestController
@RequestMapping("/api/v1/users")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class AdminUserController {
    private final AdminUserService adminUserService;

    public AdminUserController(AdminUserService adminUserService) {
        this.adminUserService = adminUserService;
    }

    @GetMapping
    public ResponseEntity<List<AdminUserResponse>> list() {
        return ResponseEntity.ok(adminUserService.list());
    }

    @PatchMapping("/{userId}/roles")
    public ResponseEntity<AdminUserResponse> updateRoles(@AuthenticationPrincipal String actorId,
            @PathVariable Long userId, @Valid @RequestBody UpdateUserRolesRequest request) {
        return ResponseEntity.ok(adminUserService.updateRoles(Long.valueOf(actorId), userId, request));
    }

    @PatchMapping("/{userId}/status")
    public ResponseEntity<AdminUserResponse> updateStatus(@AuthenticationPrincipal String actorId,
            @PathVariable Long userId, @Valid @RequestBody UpdateUserStatusRequest request) {
        return ResponseEntity.ok(adminUserService.updateStatus(Long.valueOf(actorId), userId, request.status()));
    }

    @PatchMapping("/{userId}/managed-spaces")
    public ResponseEntity<AdminUserResponse> assignManagedSpaces(@AuthenticationPrincipal String actorId,
            @PathVariable Long userId, @Valid @RequestBody AssignManagedSpacesRequest request) {
        return ResponseEntity.ok(adminUserService.assignManagedSpaces(Long.valueOf(actorId), userId, request));
    }
}
