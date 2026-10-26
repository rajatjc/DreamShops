package com.dailycodework.dreamshops.security;

import com.dailycodework.dreamshops.model.Role;
import com.dailycodework.dreamshops.repository.UserRepository;
import com.dailycodework.dreamshops.service.user.AuthService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.util.Set;

/**
 * Creates the first admin from ADMIN_EMAIL and ADMIN_PASSWORD when no account has that email yet.
 * Registration only ever creates customers, so this is how the catalog gets an owner.
 */
@Slf4j
@Component
public class AdminAccountInitializer implements ApplicationRunner {

    private final AuthService authService;
    private final UserRepository userRepository;
    private final String email;
    private final String password;

    public AdminAccountInitializer(AuthService authService, UserRepository userRepository,
                                   @Value("${app.admin.email:}") String email,
                                   @Value("${app.admin.password:}") String password) {
        this.authService = authService;
        this.userRepository = userRepository;
        this.email = email;
        this.password = password;
    }

    @Override
    public void run(ApplicationArguments args) {
        if (email.isBlank() || password.isBlank() || userRepository.existsByEmailIgnoreCase(email)) {
            return;
        }
        authService.createUser("Store", "Admin", email, password, Set.of(Role.USER, Role.ADMIN));
        log.info("Created admin account {}", email);
    }
}
