package com.example.jobplatform.controller;

import com.example.jobplatform.model.Application;
import com.example.jobplatform.model.CV;
import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.ApplicationRepository;
import com.example.jobplatform.repository.UserRepository;
import com.example.jobplatform.repository.projection.CVSummary;
import com.example.jobplatform.security.CustomOAuth2User;
import com.example.jobplatform.service.CVService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Controller
@RequiredArgsConstructor
public class CVController {

    private static final long MAX_SIZE = 20L * 1024 * 1024; // 20MB
    private static final Set<String> ALLOWED = Set.of(
            "application/pdf",
            "application/msword",
            "application/vnd.openxmlformats-officedocument.wordprocessingml.document"
    );

    private final CVService cvService;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/cv/upload")
    public String uploadForm(Authentication auth) {
        if (auth == null) return "redirect:/login";
        return "upload-cv";
    }

    @PostMapping("/cv/upload")
    public String doUpload(@RequestParam("cv") MultipartFile file,
                           Authentication auth,
                           RedirectAttributes ra) throws IOException {

        if (auth == null) return "redirect:/login";

        if (file == null || file.isEmpty()) {
            ra.addFlashAttribute("message", "Please choose a file.");
            return "redirect:/cv/upload";
        }
        if (file.getSize() > MAX_SIZE) {
            ra.addFlashAttribute("message", "File too large. Max 20 MB.");
            return "redirect:/cv/upload";
        }
        String ct = file.getContentType();
        if (ct != null && !ALLOWED.contains(ct)) {
            ra.addFlashAttribute("message", "Only PDF/DOC/DOCX are allowed.");
            return "redirect:/cv/upload";
        }

        // upload може да създаде OAuth user ако го няма
        User owner = resolveOrCreateUser(auth);

        cvService.store(file, owner);

        ra.addFlashAttribute("message", "CV uploaded.");
        return "redirect:/my-cvs";
    }

    @GetMapping("/my-cvs")
    public String list(Authentication auth, Model model) {
        if (auth == null) return "redirect:/login";

        User me = resolveUserOrThrow(auth);
        String ownerUsername = me.getUsername();

        List<CVSummary> all = cvService.findSummariesByOwner(ownerUsername);
        List<Application> apps = applicationRepository.findByCv_Owner_Username(ownerUsername);

        Set<Long> inUse = new HashSet<>();
        for (Application a : apps) {
            if (a.getCv() != null && a.getCv().getId() != null) inUse.add(a.getCv().getId());
        }

        model.addAttribute("cvs", all);
        model.addAttribute("inUse", inUse);
        return "my-cvs";
    }

    @Transactional(readOnly = true)
    @GetMapping("/cvs/{id}/download")
    public ResponseEntity<byte[]> download(@PathVariable Long id, Authentication auth) {
        if (auth == null) return ResponseEntity.status(401).build();

        CV cv = cvService.findById(id);
        if (cv == null) return ResponseEntity.notFound().build();

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        User me = resolveUserOrThrow(auth);

        boolean isOwner = cv.getOwner() != null
                && me.getId() != null
                && me.getId().equals(cv.getOwner().getId());

        if (!isOwner && !isAdmin) return ResponseEntity.status(403).build();

        return ResponseEntity.ok()
                .header("Content-Disposition", "attachment; filename=\"" + cv.getFilename() + "\"")
                .header("Content-Type", cv.getFileType() == null ? "application/octet-stream" : cv.getFileType())
                .body(cv.getData());
    }

    @Transactional
    @PostMapping("/cvs/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra, Authentication auth) {
        if (auth == null) return "redirect:/login";

        CV cv = cvService.findById(id);
        if (cv == null) {
            ra.addFlashAttribute("message", "CV not found.");
            return "redirect:/my-cvs";
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        User me = resolveUserOrThrow(auth);

        boolean isOwner = cv.getOwner() != null
                && me.getId() != null
                && me.getId().equals(cv.getOwner().getId());

        if (!isOwner && !isAdmin) {
            ra.addFlashAttribute("message", "Not allowed.");
            return "redirect:/my-cvs";
        }

        // detach apps that use this CV (only those of this owner)
        String ownerUsername = cv.getOwner() != null ? cv.getOwner().getUsername() : null;
        if (ownerUsername != null) {
            var appsUsing = applicationRepository.findByCv_Owner_Username(ownerUsername);
            boolean touched = false;
            for (Application a : appsUsing) {
                if (a.getCv() != null && id.equals(a.getCv().getId())) {
                    a.setCv(null);
                    touched = true;
                }
            }
            if (touched) applicationRepository.saveAll(appsUsing);
        }

        cvService.deleteById(id);
        ra.addFlashAttribute("message", "CV deleted.");
        return "redirect:/my-cvs";
    }

    // ✅ за действия (list/download/delete) — НЕ прави insert
    private User resolveUserOrThrow(Authentication auth) {
        Object principal = auth.getPrincipal();

        if (principal instanceof CustomOAuth2User cu) {
            return userRepository.findByEmail(cu.getEmail())
                    .or(() -> userRepository.findByUsername(cu.getEmail()))
                    .orElseThrow(() -> new IllegalStateException("User not found in DB: " + cu.getEmail()));
        }

        if (principal instanceof OAuth2User ou) {
            String email = ou.getAttribute("email");
            if (email == null || email.isBlank()) {
                throw new IllegalStateException("Google did not return email");
            }
            return userRepository.findByEmail(email)
                    .or(() -> userRepository.findByUsername(email))
                    .orElseThrow(() -> new IllegalStateException("User not found in DB: " + email));
        }

        if (principal instanceof UserDetails ud) {
            String name = ud.getUsername();
            return userRepository.findByUsername(name)
                    .or(() -> userRepository.findByEmail(name))
                    .orElseThrow(() -> new IllegalStateException("User not found in DB: " + name));
        }

        throw new IllegalStateException("Unknown principal: " + principal.getClass());
    }

    // ✅ само когато е нужно (напр. upload/profile) — може да създава OAuth user
    private User resolveOrCreateUser(Authentication auth) {
        Object principal = auth.getPrincipal();

        String email = null;

        if (principal instanceof CustomOAuth2User cu) {
            email = cu.getEmail();
        } else if (principal instanceof OAuth2User ou) {
            email = ou.getAttribute("email");
        } else if (principal instanceof UserDetails ud) {
            String name = ud.getUsername();
            return userRepository.findByUsername(name)
                    .or(() -> userRepository.findByEmail(name))
                    .orElseThrow(() -> new IllegalStateException("User not found in DB: " + name));
        }

        if (email == null || email.isBlank()) {
            throw new IllegalStateException("Cannot resolve email from principal: " + principal.getClass());
        }

        final String e = email;
        return userRepository.findByEmail(e)
                .orElseGet(() -> {
                    User u = new User();
                    u.setEmail(e);
                    u.setUsername(e);
                    u.setRole("ROLE_USER");
                    u.setPassword(passwordEncoder.encode("OAUTH_USER"));
                    return userRepository.save(u);
                });
    }
}