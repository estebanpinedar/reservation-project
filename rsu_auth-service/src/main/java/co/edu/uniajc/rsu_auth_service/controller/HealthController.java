package co.edu.uniajc.rsu_auth_service.controller;

import java.time.OffsetDateTime;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/health")
public class HealthController {
    @GetMapping
    public ResponseEntity<HealthResponse> health() {
        return ResponseEntity.ok(new HealthResponse("UP", "space-reservation-system", OffsetDateTime.now()));
    }

    public record HealthResponse(String status, String service, OffsetDateTime timestamp) {
    }
}
