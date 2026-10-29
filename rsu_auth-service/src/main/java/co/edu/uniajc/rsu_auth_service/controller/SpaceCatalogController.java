package co.edu.uniajc.rsu_auth_service.controller;

import co.edu.uniajc.rsu_auth_service.dto.SpaceResponse;
import co.edu.uniajc.rsu_auth_service.dto.SpaceTypeResponse;
import co.edu.uniajc.rsu_auth_service.dto.SaveSpaceRequest;
import co.edu.uniajc.rsu_auth_service.dto.SaveSpaceTypeRequest;
import co.edu.uniajc.rsu_auth_service.dto.UpdateSpaceStatusRequest;
import co.edu.uniajc.rsu_auth_service.dto.SpaceAgendaResponse;
import co.edu.uniajc.rsu_auth_service.model.SpaceStatus;

import java.util.List;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.net.URI;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

import org.springframework.http.ResponseEntity;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestBody;

import co.edu.uniajc.rsu_auth_service.service.SpaceCatalogService;

@Validated
@RestController
@RequestMapping("/api/v1")
public class SpaceCatalogController {
    private final SpaceCatalogService spaceCatalogService;

    public SpaceCatalogController(SpaceCatalogService spaceCatalogService) {
        this.spaceCatalogService = spaceCatalogService;
    }

    @GetMapping("/space-types")
    public ResponseEntity<List<SpaceTypeResponse>> listTypes() {
        return ResponseEntity.ok(spaceCatalogService.listTypes());
    }

    @PostMapping("/space-types")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<SpaceTypeResponse> createType(@AuthenticationPrincipal String userId,
            @jakarta.validation.Valid @RequestBody SaveSpaceTypeRequest request) {
        SpaceTypeResponse response = spaceCatalogService.createType(Long.valueOf(userId), request);
        return ResponseEntity.created(URI.create("/api/v1/space-types/" + response.id())).body(response);
    }

    @PatchMapping("/space-types/{typeId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<SpaceTypeResponse> updateType(@AuthenticationPrincipal String userId,
            @PathVariable Long typeId, @jakarta.validation.Valid @RequestBody SaveSpaceTypeRequest request) {
        return ResponseEntity.ok(spaceCatalogService.updateType(Long.valueOf(userId), typeId, request));
    }

    @GetMapping("/spaces")
    public ResponseEntity<List<SpaceResponse>> search(
            @RequestParam(required = false) @Size(max = 120, message = "Query must not exceed 120 characters") String query,
            @RequestParam(required = false) @Size(max = 80, message = "Type must not exceed 80 characters") String type,
            @RequestParam(required = false) @Min(value = 1, message = "Minimum capacity must be at least 1")
            @Max(value = 100000, message = "Minimum capacity is too large") Integer minimumCapacity) {
        return ResponseEntity.ok(spaceCatalogService.search(query, type, minimumCapacity));
    }

    @GetMapping("/spaces/manage")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<List<SpaceResponse>> searchForManagement(@AuthenticationPrincipal String userId,
            Authentication authentication,
            @RequestParam(required = false) @Size(max = 120) String query,
            @RequestParam(required = false) @Size(max = 80) String type,
            @RequestParam(required = false) @Min(1) @Max(100000) Integer minimumCapacity,
            @RequestParam(required = false) SpaceStatus status) {
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATOR"));
        return ResponseEntity.ok(spaceCatalogService.searchForManagement(Long.valueOf(userId), administrator,
                query, type, minimumCapacity, status));
    }

    @PostMapping("/spaces")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<SpaceResponse> createSpace(@AuthenticationPrincipal String userId,
            @jakarta.validation.Valid @RequestBody SaveSpaceRequest request) {
        SpaceResponse response = spaceCatalogService.createSpace(Long.valueOf(userId), request);
        return ResponseEntity.created(URI.create("/api/v1/spaces/" + response.id())).body(response);
    }

    @PatchMapping("/spaces/{spaceId}")
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<SpaceResponse> updateSpace(@AuthenticationPrincipal String userId,
            @PathVariable Long spaceId, @jakarta.validation.Valid @RequestBody SaveSpaceRequest request) {
        return ResponseEntity.ok(spaceCatalogService.updateSpace(Long.valueOf(userId), spaceId, request));
    }

    @PatchMapping("/spaces/{spaceId}/status")
    @PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
    public ResponseEntity<SpaceResponse> changeStatus(@AuthenticationPrincipal String userId,
            Authentication authentication,
            @PathVariable Long spaceId, @jakarta.validation.Valid @RequestBody UpdateSpaceStatusRequest request) {
        boolean administrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATOR"));
        return ResponseEntity.ok(spaceCatalogService.changeStatus(Long.valueOf(userId), administrator,
                spaceId, request.status()));
    }

    @GetMapping("/spaces/availability")
    public ResponseEntity<List<SpaceResponse>> findAvailable(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endAt,
            @RequestParam(required = false) @Min(1) @Max(100000) Integer minimumCapacity) {
        return ResponseEntity.ok(spaceCatalogService.findAvailable(startAt, endAt, minimumCapacity));
    }

    @GetMapping("/spaces/{spaceId}/agenda")
    public ResponseEntity<SpaceAgendaResponse> agenda(@PathVariable Long spaceId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date) {
        return ResponseEntity.ok(spaceCatalogService.agenda(spaceId, date));
    }

    @GetMapping("/spaces/{spaceId}")
    public ResponseEntity<SpaceResponse> getById(@PathVariable Long spaceId) {
        return ResponseEntity.ok(spaceCatalogService.findActiveById(spaceId));
    }
}
