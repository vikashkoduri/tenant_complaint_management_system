package com.tenantcomplaint.config;

import com.tenantcomplaint.entity.User;
import com.tenantcomplaint.repository.UserRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

/**
 * Seeds the initial reviewer account on first startup.
 */
@Component
public class DataInitializer implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Value("${app.reviewer.default-email}")
    private String defaultEmail;

    @Value("${app.reviewer.default-password}")
    private String defaultPassword;

    public DataInitializer(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public void run(String... args) {
        if (!userRepository.existsByEmail(defaultEmail)) {
            User reviewer = new User();
            reviewer.setEmail(defaultEmail);
            reviewer.setName("Admin Reviewer");
            reviewer.setPassword(passwordEncoder.encode(defaultPassword));
            reviewer.setRole("REVIEWER");
            reviewer.setEnabled(true);
            userRepository.save(reviewer);
            log.info("Default reviewer account created: {}", defaultEmail);
        } else {
            log.info("Reviewer account already exists: {}", defaultEmail);
        }
    }
}
