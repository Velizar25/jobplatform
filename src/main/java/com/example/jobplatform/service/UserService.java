package com.example.jobplatform.service;

import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.UserRepository;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserService(UserRepository userRepository,
                       PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    public User register(User user) {
        if (user == null) {
            throw new IllegalArgumentException("Invalid user");
        }

        String username = safe(user.getUsername());
        String email = normalizeEmail(user.getEmail());
        String rawPassword = user.getPassword();

        if (username.isEmpty()) {
            throw new IllegalArgumentException("Username is required");
        }

        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        if (rawPassword == null || rawPassword.length() < 4) {
            throw new IllegalArgumentException("Password must be at least 4 characters");
        }

        if (userRepository.existsByUsername(username)) {
            throw new IllegalArgumentException("Username already taken");
        }

        if (userRepository.existsByEmail(email)) {
            throw new IllegalArgumentException("Email already taken (maybe created via Google login)");
        }

        user.setUsername(username);
        user.setEmail(email);
        user.setRole("ROLE_USER");
        user.setPassword(passwordEncoder.encode(rawPassword));

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Username or email already exists");
        }
    }

    public User updateProfile(Long userId, String newEmail, String newRawPassword) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new IllegalArgumentException("User not found with ID: " + userId));

        String email = normalizeEmail(newEmail);

        if (email.isEmpty()) {
            throw new IllegalArgumentException("Email is required");
        }

        userRepository.findByEmail(email).ifPresent(existing -> {
            if (!existing.getId().equals(user.getId())) {
                throw new IllegalArgumentException("Email is already used by another account");
            }
        });

        user.setEmail(email);

        if (newRawPassword != null && !newRawPassword.isBlank()) {
            if (newRawPassword.length() < 4) {
                throw new IllegalArgumentException("Password must be at least 4 characters");
            }

            user.setPassword(passwordEncoder.encode(newRawPassword));
        }

        try {
            return userRepository.save(user);
        } catch (DataIntegrityViolationException e) {
            throw new IllegalArgumentException("Email is already used by another account");
        }
    }

    private String safe(String value) {
        return value == null ? "" : value.trim();
    }

    private String normalizeEmail(String email) {
        return safe(email).toLowerCase();
    }
}