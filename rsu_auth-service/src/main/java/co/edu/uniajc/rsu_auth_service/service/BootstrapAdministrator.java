package co.edu.uniajc.rsu_auth_service.service;

import java.util.Locale;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import co.edu.uniajc.rsu_auth_service.model.Role;
import co.edu.uniajc.rsu_auth_service.model.RoleName;
import co.edu.uniajc.rsu_auth_service.model.User;
import co.edu.uniajc.rsu_auth_service.repository.RoleRepository;
import co.edu.uniajc.rsu_auth_service.repository.UserRepository;

@Component
public class BootstrapAdministrator implements ApplicationRunner {
    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final String name;
    private final String email;
    private final String password;

    public BootstrapAdministrator(UserRepository userRepository, RoleRepository roleRepository,
            PasswordEncoder passwordEncoder,
            @Value("${app.bootstrap-admin.name:}") String name,
            @Value("${app.bootstrap-admin.email:}") String email,
            @Value("${app.bootstrap-admin.password:}") String password) {
        this.userRepository = userRepository;
        this.roleRepository = roleRepository;
        this.passwordEncoder = passwordEncoder;
        this.name = name;
        this.email = email;
        this.password = password;
    }

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (name.isBlank() && email.isBlank() && password.isBlank()) {
            return;
        }
        if (name.isBlank() || email.isBlank() || password.length() < 12) {
            throw new IllegalStateException("Configure APP_BOOTSTRAP_ADMIN_NAME, EMAIL, and a PASSWORD of at least 12 characters");
        }

        String canonicalEmail = email.trim().toLowerCase(Locale.ROOT);
        Role administratorRole = roleRepository.findByName(RoleName.ADMINISTRATOR)
                .orElseThrow(() -> new IllegalStateException("ADMINISTRATOR role is missing from the database"));
        User user = userRepository.findByEmailIgnoreCase(canonicalEmail)
                .orElseGet(() -> new User(name.trim(), canonicalEmail, passwordEncoder.encode(password), administratorRole));
        user.addRole(administratorRole);
        userRepository.save(user);
    }
}
