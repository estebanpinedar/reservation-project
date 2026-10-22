package co.edu.uniajc.rsu_auth_service.controller;

import java.time.OffsetDateTime;
import java.util.List;

import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniajc.rsu_auth_service.dto.AuditEntryResponse;
import co.edu.uniajc.rsu_auth_service.dto.SpaceUsageResponse;
import co.edu.uniajc.rsu_auth_service.service.ReportingService;

@RestController
@RequestMapping("/api/v1")
@PreAuthorize("hasRole('ADMINISTRATOR')")
public class ReportingController {
    private final ReportingService reportingService;

    public ReportingController(ReportingService reportingService) {
        this.reportingService = reportingService;
    }

    @GetMapping("/reports/usage")
    public ResponseEntity<List<SpaceUsageResponse>> usage(
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime startAt,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME) OffsetDateTime endAt) {
        return ResponseEntity.ok(reportingService.usage(startAt, endAt));
    }

    @GetMapping("/audit")
    public ResponseEntity<List<AuditEntryResponse>> recentAudit() {
        return ResponseEntity.ok(reportingService.recentAudit());
    }
}
