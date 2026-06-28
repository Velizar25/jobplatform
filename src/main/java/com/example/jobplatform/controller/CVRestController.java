package com.example.jobplatform.controller;

import com.example.jobplatform.model.Application;
import com.example.jobplatform.model.CV;
import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.ApplicationRepository;
import com.example.jobplatform.repository.UserRepository;
import com.example.jobplatform.repository.projection.CVSummary;
import com.example.jobplatform.security.CustomOAuth2User;
import com.example.jobplatform.service.CVService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

@RestController
@RequestMapping("/api/cvs")
public class CVRestController {

    private static final long MAX_SIZE = 20L * 1024 * 1024; // 20MB

    private static final Set<String> ALLOWED_TYPES = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final CVService cvService;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public CVRestController(CVService cvService,
                            ApplicationRepository applicationRepository,
                            UserRepository userRepository,
                            PasswordEncoder passwordEncoder) {
        this.cvService = cvService;
        this.applicationRepository = applicationRepository;
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    // -------------------------
    // UPLOAD CV
    // -------------------------
    @PostMapping("/upload")
    public ResponseEntity<Map<String, String>> upload(@RequestParam("cv") MultipartFile file,
                                                      Authentication auth) throws IOException {

        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Please choose a file");
        }

        if (file.getSize() > MAX_SIZE) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "File too large. Max 20 MB");
        }

        String contentType = file.getContentType();

        if (contentType == null || !ALLOWED_TYPES.contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "Only PDF, DOC and DOCX files are allowed");
        }

        User owner = resolveOrCreateUser(auth);

        cvService.store(file, owner);

        return ResponseEntity.ok(Map.of("message", "CV uploaded successfully"));
    }

    // -------------------------
    // LIST MY CVS
    // -------------------------
    @GetMapping
    public List<CVResponse> listMyCvs(Authentication auth) {
        User currentUser = resolveUserOrThrow(auth);
        String ownerUsername = currentUser.getUsername();

        List<CVSummary> cvs = cvService.findSummariesByOwner(ownerUsername);
        List<Application> applications = applicationRepository.findByCv_Owner_Username(ownerUsername);

        Set<Long> inUse = new HashSet<>();

        for (Application application : applications) {
            if (application.getCv() != null && application.getCv().getId() != null) {
                inUse.add(application.getCv().getId());
            }
        }

        return cvs.stream()
                .map(cv -> new CVResponse(
                        cv.getId(),
                        cv.getFilename(),
                        cv.getFileType(),
                        inUse.contains(cv.getId())
                ))
                .toList();
    }

    // -------------------------
    // DOWNLOAD CV
    // -------------------------
    @Transactional(readOnly = true)
    @GetMapping("/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id,
                                           Authentication auth) {

        User currentUser = resolveUserOrThrow(auth);

        CV cv = cvService.findById(id);

        if (cv == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found");
        }

        boolean isAdmin = isAdmin(auth);

        boolean isOwner = cv.getOwner() != null
                && currentUser.getId() != null
                && currentUser.getId().equals(cv.getOwner().getId());

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to download this CV");
        }

        String filename = cv.getFilename() == null ? "cv" : cv.getFilename();
        String fileType = cv.getFileType() == null ? "application/octet-stream" : cv.getFileType();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + filename + "\"")
                .header("Content-Type", fileType)
                .body(cv.getData());
    }

    // -------------------------
    // DELETE CV
    // -------------------------
    @Transactional
    @DeleteMapping("/{id}")
    public ResponseEntity<Map<String, String>> delete(@PathVariable Long id,
                                                      Authentication auth) {

        User currentUser = resolveUserOrThrow(auth);

        CV cv = cvService.findById(id);

        if (cv == null) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "CV not found");
        }

        boolean isAdmin = isAdmin(auth);

        boolean isOwner = cv.getOwner() != null
                && currentUser.getId() != null
                && currentUser.getId().equals(cv.getOwner().getId());

        if (!isOwner && !isAdmin) {
            throw new ResponseStatusException(HttpStatus.FORBIDDEN, "Not allowed to delete this CV");
        }

        String ownerUsername = cv.getOwner() != null ? cv.getOwner().getUsername() : null;

        if (ownerUsername != null) {
            List<Application> applicationsUsingCv = applicationRepository.findByCv_Owner_Username(ownerUsername);

            boolean changed = false;

            for (Application application : applicationsUsingCv) {
                if (application.getCv() != null && id.equals(application.getCv().getId())) {
                    application.setCv(null);
                    changed = true;
                }
            }

            if (changed) {
                applicationRepository.saveAll(applicationsUsingCv);
            }
        }

        cvService.deleteById(id);

        return ResponseEntity.ok(Map.of("message", "CV deleted successfully"));
    }

    // -------------------------
    // HELPERS
    // -------------------------
    private User resolveUserOrThrow(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Object principal = auth.getPrincipal();

        if (principal instanceof CustomOAuth2User customOAuth2User) {
            String email = customOAuth2User.getEmail();

            return userRepository.findByEmail(email)
                    .or(() -> userRepository.findByUsername(email))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }

        if (principal instanceof OAuth2User oauth2User) {
            String email = oauth2User.getAttribute("email");

            if (email == null || email.isBlank()) {
                throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Google did not return email");
            }

            return userRepository.findByEmail(email)
                    .or(() -> userRepository.findByUsername(email))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }

        if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();

            return userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }

        throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Unknown principal");
    }

    private User resolveOrCreateUser(Authentication auth) {
        if (auth == null) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Not authenticated");
        }

        Object principal = auth.getPrincipal();

        String email = null;

        if (principal instanceof CustomOAuth2User customOAuth2User) {
            email = customOAuth2User.getEmail();
        } else if (principal instanceof OAuth2User oauth2User) {
            email = oauth2User.getAttribute("email");
        } else if (principal instanceof UserDetails userDetails) {
            String username = userDetails.getUsername();

            return userRepository.findByUsername(username)
                    .or(() -> userRepository.findByEmail(username))
                    .orElseThrow(() -> new ResponseStatusException(HttpStatus.UNAUTHORIZED, "User not found"));
        }

        if (email == null || email.isBlank()) {
            throw new ResponseStatusException(HttpStatus.UNAUTHORIZED, "Cannot resolve email from principal");
        }

        String finalEmail = email;

        return userRepository.findByEmail(finalEmail)
                .orElseGet(() -> {
                    User user = new User();
                    user.setEmail(finalEmail);
                    user.setUsername(finalEmail);
                    user.setRole("ROLE_USER");
                    user.setPassword(passwordEncoder.encode("OAUTH_USER"));
                    return userRepository.save(user);
                });
    }

    private boolean isAdmin(Authentication auth) {
        return auth != null && auth.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));
    }

    // -------------------------
    // DTO
    // -------------------------
    public record CVResponse(
            Long id,
            String filename,
            String fileType,
            boolean inUse
    ) {
    }
}