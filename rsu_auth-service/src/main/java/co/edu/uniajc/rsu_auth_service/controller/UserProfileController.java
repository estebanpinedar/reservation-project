package co.edu.uniajc.rsu_auth_service.controller;


import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import co.edu.uniajc.rsu_auth_service.service.AuthService;

@RestController
@RequestMapping("/api/v1/users")
public class UserProfileController {
    private final AuthService authService;

    public UserProfileController(AuthService authService) {
        this.authService = authService;
    }

    @GetMapping("/me")
    public ResponseEntity<AuthService.UserProfile> profile(@AuthenticationPrincipal String userId) {
        return ResponseEntity.ok(authService.profile(Long.valueOf(userId)));
    }
}
