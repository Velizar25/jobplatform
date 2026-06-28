package com.example.jobplatform.controller;

import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.UserRepository;
import com.example.jobplatform.security.CustomOAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/profile")
public class ProfileRestController {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public ProfileRestController(UserRepository userRepository,
                                 PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // -------------------------
    // GET CURRENT USER PROFILE
    // -------------------------
    @GetMapping
    public ProfileResponse getProfile(Authentication auth) {
        User user = resolveUser(auth);
        boolean isOAuth = isOAuthUser(auth);

        return new ProfileResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                user.getFullName(),
                user.getSkills(),
                user.getPreferredLocation(),
                user.getPreferredJobType(),
                isOAuth
        );
    }

    // -------------------------
    // UPDATE CURRENT USER PROFILE
    // -------------------------
    @PutMapping
    public ResponseEntity<Map<String, String>> updateProfile(Authentication auth,
                                                             @RequestBody UpdateProfileRequest request) {
        User user = resolveUser(auth);
        boolean isOAuth = isOAuthUser(auth);

        if (request.email() == null || request.email().isBlank()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Email is required");
        }

        if (!isOAuth && request.password() != null && !request.password().isBlank()) {
            if (!request.password().equals(request.confirmPassword())) {
                throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Passwords do not match");
            }

            user.setPassword(passwordEncoder.encode(request.password()));
        }

        user.setEmail(trim(request.email()));
        user.setFullName(trim(request.fullName()));
        user.setSkills(trim(request.skills()));
        user.setPreferredLocation(trim(request.preferredLocation()));
        user.setPreferredJobType(trim(request.preferredJobType()));

        try {
            userRepository.save(user);
            return ResponseEntity.ok(Map.of("message", "Profile updated successfully"));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Profile update failed: " + e.getMessage());
        }
    }

    // -------------------------
    // HELPERS
    // -------------------------
    private User resolveUser(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Object principal = auth.getPrincipal();
        String identity;

        if (principal instanceof CustomOAuth2User customOAuth2User) {
            identity = customOAuth2User.getEmail();
        } else if (principal instanceof OAuth2User oauth2User) {
            identity = oauth2User.getAttribute("email");
        } else if (principal instanceof UserDetails userDetails) {
            identity = userDetails.getUsername();
        } else {
            identity = auth.getName();
        }

        if (identity == null || identity.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing identity");
        }

        String finalIdentity = identity;

        return userRepository.findByUsername(finalIdentity)
                .or(() -> userRepository.findByEmail(finalIdentity))
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(finalIdentity);
                    user.setUsername(finalIdentity);
                    user.setRole("ROLE_USER");
                    user.setPassword(passwordEncoder.encode("OAUTH_USER"));
                    return userRepository.save(user);
                });
    }

    private boolean isOAuthUser(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            return false;
        }

        Object principal = auth.getPrincipal();

        return principal instanceof OAuth2User || principal instanceof CustomOAuth2User;
    }

    private String trim(String value) {
        return value == null ? null : value.trim();
    }

    // -------------------------
    // DTOs
    // -------------------------
    public record ProfileResponse(
            Long id,
            String username,
            String email,
            String role,
            String fullName,
            String skills,
            String preferredLocation,
            String preferredJobType,
            boolean oauthUser
    ) {
    }

    public record UpdateProfileRequest(
            String email,
            String password,
            String confirmPassword,
            String fullName,
            String skills,
            String preferredLocation,
            String preferredJobType
    ) {
    }
}