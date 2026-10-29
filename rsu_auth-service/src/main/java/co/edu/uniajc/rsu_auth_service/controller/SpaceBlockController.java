package co.edu.uniajc.rsu_auth_service.controller;

import java.net.URI;
import java.util.List;
import java.time.OffsetDateTime;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.format.annotation.DateTimeFormat;

import co.edu.uniajc.rsu_auth_service.dto.CreateSpaceBlockRequest;
import co.edu.uniajc.rsu_auth_service.dto.SpaceBlockResponse;
import co.edu.uniajc.rsu_auth_service.model.BlockStatus;
import co.edu.uniajc.rsu_auth_service.service.SpaceBlockService;

@RestController
@RequestMapping("/api/v1/space-blocks")
@PreAuthorize("hasAnyRole('ADMINISTRATOR', 'MANAGER')")
public class SpaceBlockController {
    private final SpaceBlockService spaceBlockService;

    public SpaceBlockController(SpaceBlockService spaceBlockService) {
        this.spaceBlockService = spaceBlockService;
    }

    @PostMapping
    public ResponseEntity<SpaceBlockResponse> create(@AuthenticationPrincipal String userId, Authentication authentication,
            @Valid @RequestBody CreateSpaceBlockRequest request) {
        SpaceBlockResponse response = spaceBlockService.create(Long.valueOf(userId), isAdministrator(authentication), request);
        return ResponseEntity.created(URI.create("/api/v1/space-blocks/" + response.id())).body(response);
    }

    @GetMapping
    public ResponseEntity<List<SpaceBlockResponse>> list(@AuthenticationPrincipal String userId,
            Authentication authentication, @RequestParam(required = false) Long spaceId,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endAt,
            @RequestParam(required = false) BlockStatus status) {
        return ResponseEntity.ok(spaceBlockService.list(Long.valueOf(userId), isAdministrator(authentication),
                spaceId, startAt, endAt, status));
    }

    @PatchMapping("/{blockId}/cancel")
    public ResponseEntity<SpaceBlockResponse> cancel(@AuthenticationPrincipal String userId, Authentication authentication,
            @PathVariable Long blockId) {
        return ResponseEntity.ok(spaceBlockService.cancel(blockId, Long.valueOf(userId), isAdministrator(authentication)));
    }

    private boolean isAdministrator(Authentication authentication) {
        return authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATOR"));
    }
}
