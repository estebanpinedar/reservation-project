package co.edu.uniajc.rsu_auth_service.controller;

import co.edu.uniajc.rsu_auth_service.dto.CancelReservationRequest;
import co.edu.uniajc.rsu_auth_service.dto.CreateReservationRequest;
import co.edu.uniajc.rsu_auth_service.dto.ReservationResponse;
import co.edu.uniajc.rsu_auth_service.dto.ReservationOverviewResponse;
import co.edu.uniajc.rsu_auth_service.model.ReservationStatus;

import java.net.URI;
import java.util.List;
import java.time.OffsetDateTime;

import jakarta.validation.Valid;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.Authentication;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestParam;

import co.edu.uniajc.rsu_auth_service.service.ReservationService;

@RestController
@RequestMapping("/api/v1/reservations")
public class ReservationController {
    private final ReservationService reservationService;

    public ReservationController(ReservationService reservationService) {
        this.reservationService = reservationService;
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('USER', 'MANAGER', 'ADMINISTRATOR')")
    public ResponseEntity<ReservationResponse> create(@AuthenticationPrincipal String userId,
            @Valid @RequestBody CreateReservationRequest request) {
        ReservationResponse response = reservationService.create(Long.valueOf(userId), request);
        return ResponseEntity.created(URI.create("/api/v1/reservations/" + response.id())).body(response);
    }

    @GetMapping("/me")
    public ResponseEntity<List<ReservationResponse>> listMine(@AuthenticationPrincipal String userId,
            @RequestParam(required = false) ReservationStatus status,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endAt) {
        return ResponseEntity.ok(reservationService.listMine(Long.valueOf(userId), status, startAt, endAt));
    }

    @GetMapping
    @PreAuthorize("hasRole('ADMINISTRATOR')")
    public ResponseEntity<List<ReservationOverviewResponse>> listAgenda(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endAt,
            @RequestParam(required = false) Long spaceId,
            @RequestParam(required = false) ReservationStatus status) {
        return ResponseEntity.ok(reservationService.listAgenda(startAt, endAt, spaceId, status));
    }

    @PatchMapping("/{reservationId}/cancel")
    public ResponseEntity<ReservationResponse> cancel(@PathVariable Long reservationId,
            @AuthenticationPrincipal String userId, Authentication authentication,
            @Valid @RequestBody CancelReservationRequest request) {
        boolean isAdministrator = authentication.getAuthorities().stream()
                .anyMatch(authority -> authority.getAuthority().equals("ROLE_ADMINISTRATOR"));
        return ResponseEntity.ok(reservationService.cancel(reservationId, Long.valueOf(userId), isAdministrator, request));
    }
}
