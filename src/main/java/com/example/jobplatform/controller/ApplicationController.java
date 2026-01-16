package com.example.jobplatform.controller;

import com.example.jobplatform.model.Application;
import com.example.jobplatform.repository.ApplicationRepository;
import com.example.jobplatform.security.CustomOAuth2User;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.oauth2.core.user.OAuth2User;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequiredArgsConstructor
public class ApplicationController {

    private final ApplicationRepository applicationRepository;

    @GetMapping("/applications")
    public String list(Model model, Authentication auth) {
        if (auth == null) return "redirect:/login";

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        if (isAdmin) {
            model.addAttribute("apps", applicationRepository.findAllByOrderByIdDesc());
        } else {
            String username = resolveUsername(auth);
            model.addAttribute("apps", applicationRepository.findByApplicant_UsernameOrderByIdDesc(username));
        }

        return "applications";
    }

    @PostMapping("/applications/{id}/delete")
    public String delete(@PathVariable Long id, Authentication auth, RedirectAttributes ra) {
        if (auth == null) return "redirect:/login";

        Application app = applicationRepository.findById(id).orElse(null);
        if (app == null) {
            ra.addFlashAttribute("message", "Application not found.");
            return "redirect:/applications";
        }

        boolean isAdmin = auth.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_ADMIN"));

        String username = resolveUsername(auth);

        boolean isOwner = app.getApplicant() != null
                && app.getApplicant().getUsername() != null
                && app.getApplicant().getUsername().equals(username);

        if (!isOwner && !isAdmin) {
            ra.addFlashAttribute("message", "Not allowed.");
            return "redirect:/applications";
        }

        applicationRepository.deleteById(id);
        ra.addFlashAttribute("message", "Application deleted.");
        return "redirect:/applications";
    }

    private String resolveUsername(Authentication auth) {
        Object p = auth.getPrincipal();

        if (p instanceof CustomOAuth2User cu) return cu.getUsername();
        if (p instanceof UserDetails ud) return ud.getUsername();

        if (p instanceof OAuth2User ou) {
            String email = ou.getAttribute("email");
            if (email != null && !email.isBlank()) return email;
        }

        return auth.getName();
    }
}