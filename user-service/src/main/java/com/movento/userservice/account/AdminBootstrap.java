package com.movento.userservice.account;

import com.movento.userservice.model.ERole;
import com.movento.userservice.model.Role;
import com.movento.userservice.model.User;
import com.movento.userservice.model.ViewerProfile;
import com.movento.userservice.repository.RoleRepository;
import com.movento.userservice.repository.UserRepository;
import com.movento.userservice.repository.ViewerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.HashSet;
import java.util.Set;

/**
 * Creates the admin account from ADMIN_EMAIL / ADMIN_PASSWORD at startup.
 * Admin rights are never granted through public sign-up, so nobody can claim them by registering first.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class AdminBootstrap implements ApplicationRunner {
    private final UserRepository users;
    private final RoleRepository roles;
    private final ViewerProfileRepository profiles;
    private final PasswordEncoder encoder;

    @Value("${app.admin-email:}")
    private String adminEmail;

    @Value("${app.admin-password:}")
    private String adminPassword;

    @Override
    @Transactional
    public void run(ApplicationArguments args) {
        if (adminEmail.isBlank() || adminPassword.isBlank()) {
            log.info("ADMIN_EMAIL/ADMIN_PASSWORD not set: no admin account bootstrapped");
            return;
        }
        String email = adminEmail.trim().toLowerCase();
        if (users.existsByEmail(email)) return;
        if (adminPassword.length() < 8) {
            log.warn("ADMIN_PASSWORD must be at least 8 characters: admin account not created");
            return;
        }
        Set<Role> granted = new HashSet<>();
        granted.add(roles.findByName(ERole.ROLE_ADMIN).orElseThrow());
        granted.add(roles.findByName(ERole.ROLE_USER).orElseThrow());
        User admin = new User(email, encoder.encode(adminPassword), "Movento", "Admin");
        admin.setRoles(granted);
        admin.setEmailVerified(true);
        admin = users.save(admin);
        ViewerProfile profile = new ViewerProfile();
        profile.setUser(admin);
        profile.setName("Admin");
        profile.setMaturityLevel("ADULT");
        profiles.save(profile);
        log.info("Bootstrapped admin account {}", email);
    }
}
