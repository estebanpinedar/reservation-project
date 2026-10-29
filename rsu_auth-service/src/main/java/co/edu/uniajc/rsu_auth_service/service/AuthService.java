package co.edu.uniajc.rsu_auth_service.service;

import java.util.Comparator;
import java.util.Locale;

import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniajc.rsu_auth_service.dto.AuthResponse;
import co.edu.uniajc.rsu_auth_service.dto.LoginRequest;
import co.edu.uniajc.rsu_auth_service.dto.RegisterRequest;
import co.edu.uniajc.rsu_auth_service.exeption.ConflictException;
import co.edu.uniajc.rsu_auth_service.exeption.InvalidCredentialsException;
import co.edu.uniajc.rsu_auth_service.security.JwtService;
import co.edu.uniajc.rsu_auth_service.model.Role;
import co.edu.uniajc.rsu_auth_service.model.RoleName;
import co.edu.uniajc.rsu_auth_service.model.User;
import co.edu.uniajc.rsu_auth_service.model.UserStatus;
import co.edu.uniajc.rsu_auth_service.repository.RoleRepository;
import co.edu.uniajc.rsu_auth_service.repository.UserRepository;

@Service
public class AuthService {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;

    public AuthService(UserRepository userRepository, RoleRepository roleRepository, PasswordEncoder passwordEncoder,
            JwtService jwtService) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.jwtService = jwtService;
    }

    @Transactional
    public AuthResponse register(RegisterRequest request) {
        String email = canonicalEmail(request.email());
        if (userRepository.existsByEmailIgnoreCase(email)) {
            throw new ConflictException("An account with this email already exists");
        }
        Role userRole = roleRepository.findByName(RoleName.USER)
                .orElseThrow(() -> new IllegalStateException("USER role is missing from the database"));
        User user = userRepository.save(new User(request.name().trim(), email, passwordEncoder.encode(request.password()), userRole));
        return responseFor(user);
    }

    @Transactional(readOnly = true)
    public AuthResponse login(LoginRequest request) {
        User user = userRepository.findByEmailIgnoreCase(canonicalEmail(request.email()))
                .filter(candidate -> candidate.getStatus() == UserStatus.ACTIVE)
                .filter(candidate -> passwordEncoder.matches(request.password(), candidate.getPasswordHash()))
                .orElseThrow(InvalidCredentialsException::new);
        return responseFor(user);
    }

    @Transactional(readOnly = true)
    public UserProfile profile(Long userId) {
        User user = userRepository.findWithRolesAndManagedSpacesById(userId)
                .orElseThrow(() -> new InvalidCredentialsException());
        return new UserProfile(user.getId(), user.getName(), user.getEmail(), user.getStatus().name(),
                user.getRoles().stream().map(Role::getName).map(Enum::name).sorted().toList(),
                user.getManagedSpaces().stream().map(co.edu.uniajc.rsu_auth_service.model.Space::getId).sorted().toList());
    }

    private AuthResponse responseFor(User user) {
        JwtService.Token token = jwtService.createToken(user);
        return new AuthResponse(token.value(), "Bearer", token.expiresAt(), user.getId(), user.getName(), user.getEmail(),
                user.getRoles().stream().map(Role::getName).map(Enum::name).sorted(Comparator.naturalOrder()).toList());
    }

    private String canonicalEmail(String email) {
        return email.trim().toLowerCase(Locale.ROOT);
    }

    public record UserProfile(Long id, String name, String email, String status, java.util.List<String> roles,
            java.util.List<Long> managedSpaceIds) {
    }
}
