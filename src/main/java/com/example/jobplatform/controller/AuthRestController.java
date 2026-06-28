package com.example.jobplatform.controller;

import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.UserRepository;
import com.example.jobplatform.security.CustomOAuth2User;
import com.example.jobplatform.service.UserService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AnonymousAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.Map;

@RestController
@RequestMapping("/api/auth")
public class AuthRestController {

    private final UserService userService;
    private final UserRepository userRepository;

    public AuthRestController(UserService userService,
                              UserRepository userRepository) {
        this.userService = userService;
        this.userRepository = userRepository;
    }

    // -------------------------
    // REGISTER
    // -------------------------
    @PostMapping("/register")
    public ResponseEntity<Map<String, String>> register(@RequestBody RegisterRequest request) {
        try {
            User user = new User();
            user.setUsername(request.username());
            user.setEmail(request.email());
            user.setPassword(request.password());
            user.setRole("ROLE_USER");

            userService.register(user);

            return ResponseEntity.status(HttpStatus.CREATED)
                    .body(Map.of("message", "Registration successful"));
        } catch (Exception e) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, e.getMessage());
        }
    }

    // -------------------------
    // CURRENT LOGGED USER
    // -------------------------
    @GetMapping("/me")
    public UserResponse currentUser(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null || auth instanceof AnonymousAuthenticationToken) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        User user = resolveUser(auth);

        return new UserResponse(
                user.getId(),
                user.getUsername(),
                user.getEmail(),
                user.getRole(),
                isOAuthUser(auth)
        );
    }

    // -------------------------
    // SIMPLE AUTH CHECK
    // -------------------------
    @GetMapping("/status")
    public Map<String, Object> status(Authentication auth) {
        boolean authenticated = auth != null
                && auth.isAuthenticated()
                && !(auth instanceof AnonymousAuthenticationToken);

        return Map.of(
                "authenticated", authenticated
        );
    }

    // -------------------------
    // HELPERS
    // -------------------------
    private User resolveUser(Authentication auth) {
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
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Missing user identity");
        }

        return userRepository.findByUsername(identity)
                .or(() -> userRepository.findByEmail(identity))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private boolean isOAuthUser(Authentication auth) {
        Object principal = auth.getPrincipal();

        return principal instanceof OAuth2User || principal instanceof CustomOAuth2User;
    }

    // -------------------------
    // DTOs
    // -------------------------
    public record RegisterRequest(
            String username,
            String email,
            String password
    ) {
    }

    public record UserResponse(
            Long id,
            String username,
            String email,
            String role,
            boolean oauthUser
    ) {
    }
}
