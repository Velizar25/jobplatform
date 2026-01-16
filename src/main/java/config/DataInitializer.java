package com.example.jobplatform.config;

import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.crypto.password.PasswordEncoder;

@Configuration
@RequiredArgsConstructor
public class DataInitializer implements CommandLineRunner {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) {

        String adminEmail = "admin@jobplatform.com";

        userRepository.findByEmail(adminEmail).orElseGet(() -> {
            User admin = new User();
            admin.setEmail(adminEmail);
            admin.setUsername("admin");
            admin.setRole("ROLE_ADMIN");
            admin.setPassword(passwordEncoder.encode("1234"));
            return userRepository.save(admin);
        });
    }
}