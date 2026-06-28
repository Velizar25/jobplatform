package com.example.jobplatform.controller;

import com.example.jobplatform.model.Application;
import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.ApplicationRepository;
import com.example.jobplatform.repository.UserRepository;
import com.example.jobplatform.security.CustomOAuth2User;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/applications")
public class ApplicationRestController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    public ApplicationRestController(ApplicationRepository applicationRepository,
                                     UserRepository userRepository) {
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
    }

    // -------------------------
    // LIST APPLICATIONS
    // Admin вижда всички, обикновен потребител вижда само своите
    // -------------------------
    @GetMapping
    public List<ApplicationResponse> list(Authentication auth) {
        User currentUser = getCurrentUser(auth);

        boolean isAdmin = isAdmin(auth);

        List<Application> applications;

        if (isAdmin) {
            applications = applicationRepository.findAllByOrderByIdDesc();
        } else {
            applications = applicationRepository.findByApplicant_UsernameOrderByIdDesc(currentUser.getUsername());
        }

        return applications.stream()
                .map(this::toApplicationResponse)
                .toList();
    }

    // -------------------------
    // DELETE APPLICATION
    // Admin може да трие всички, потребителят само своите
    // -------------------------
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id,
                                                      Authentication auth) {

        User currentUser = getCurrentUser(auth);

        Application application = applicationRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "Application not found"));

        boolean isAdmin = isAdmin(auth);

        boolean isOwner = application.getApplicant() != null
                && application.getApplicant().getId() != null
                && application.getApplicant().getId().equals(currentUser.getId());

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this application");
        }

        applicationRepository.deleteById(id);

        return ResponseEntity.ok(Map.of("message", "Application deleted successfully"));
    }

    // -------------------------
    // HELPERS
    // -------------------------
    private User getCurrentUser(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        String username = resolveUsername(auth);

        return userRepository.findByUsername(username)
                .or(() -> userRepository.findByEmail(username))
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    private String resolveUsername(Authentication auth) {
        Object principal = auth.getPrincipal();

        if (principal instanceof CustomOAuth2User customOAuth2User) {
            return customOAuth2User.getUsername();
        }

        if (principal instanceof UserDetails userDetails) {
            return userDetails.getUsername();
        }

        if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");

            if (email != null && !email.isBlank()) {
                return email;
            }
        }

        return auth.getName();
    }

    private ApplicationResponse toApplicationResponse(Application application) {
        Long jobId = application.getJob() != null ? application.getJob().getId() : null;
        Long cvId = application.getCv() != null ? application.getCv().getId() : null;

        String applicantUsername = application.getApplicant() != null
                ? application.getApplicant().getUsername()
                : null;

        return new ApplicationResponse(
                application.getId(),
                application.getJobTitle(),
                application.getEmail(),
                application.getStatus(),
                jobId,
                cvId,
                applicantUsername
        );
    }

    // -------------------------
    // DTO
    // -------------------------
    public record ApplicationResponse(
            Long id,
            String jobTitle,
            String email,
            String status,
            Long jobId,
            Long cvId,
            String applicantUsername
    ) {
    }
}