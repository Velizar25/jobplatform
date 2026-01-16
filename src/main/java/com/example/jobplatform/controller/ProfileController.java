package com.example.jobplatform.controller;

import com.example.jobplatform.model.User;
import com.example.jobplatform.repository.UserRepository;
import com.example.jobplatform.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ProfileController {

    private final UserService userService;
    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    @GetMapping("/profile")
    public String showProfile(Model model, Authentication auth) {
        User user = resolveUser(auth);

        boolean isOAuth = auth != null && auth.getPrincipal() instanceof OAuth2User;

        model.addAttribute("user", user);
        model.addAttribute("isOAuth", isOAuth);

        return "profile";
    }

    @PostMapping("/profile")
    public String updateProfile(Authentication auth,
                                @RequestParam String email,
                                @RequestParam(required = false) String password,
                                @RequestParam(required = false) String confirmPassword) {

        User user = resolveUser(auth);
        boolean isOAuth = auth != null && auth.getPrincipal() instanceof OAuth2User;

        try {
            if (isOAuth) {
                userService.updateProfile(user.getId(), email, null);
                return "redirect:/profile?success";
            }

            if (password != null && !password.isBlank() && !password.equals(confirmPassword)) {
                return "redirect:/profile?error=Passwords%20do%20not%20match";
            }

            userService.updateProfile(user.getId(), email, password);
            return "redirect:/profile?success";

        } catch (Exception e) {
            return "redirect:/profile?error=" + e.getMessage();
        }
    }

    private User resolveUser(Authentication auth) {
        if (auth == null || auth.getPrincipal() == null) {
            throw new IllegalStateException("Not authenticated");
        }

        Object principal = auth.getPrincipal();
        String identity;

        if (principal instanceof OAuth2User oauth) {
            identity = oauth.getAttribute("email");
        } else if (principal instanceof UserDetails ud) {
            identity = ud.getUsername(); // това е username (при form login)
        } else {
            identity = auth.getName();
        }

        if (identity == null || identity.isBlank()) {
            throw new IllegalStateException("Missing identity");
        }

        return userRepository.findByUsername(identity)
                .or(() -> userRepository.findByEmail(identity))
                .orElseGet(() -> {
                    // ако е OAuth user и липсва в DB
                    User u = new User();
                    u.setEmail(identity);
                    u.setUsername(identity);
                    u.setRole("ROLE_USER");
                    u.setPassword(passwordEncoder.encode("OAUTH_USER"));
                    return userRepository.save(u);
                });
    }
}