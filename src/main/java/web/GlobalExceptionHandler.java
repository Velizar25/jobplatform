package com.example.jobplatform.web;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.net.URI;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public String handleMaxUpload(MaxUploadSizeExceededException ex,
                                  HttpServletRequest request,
                                  RedirectAttributes ra) {
        ra.addFlashAttribute("message", "File too large. Max allowed is 20 MB.");

        return redirectBack(request, "/cv/upload");
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public String handleDataIntegrity(DataIntegrityViolationException ex,
                                      HttpServletRequest request,
                                      RedirectAttributes ra) {
        String msg = "Operation blocked by database constraints.";
        String root = String.valueOf(ex.getMostSpecificCause()).toLowerCase();
        if (root.contains("foreign key constraint")) {
            // типично: триене на CV, което се използва в applications
            msg = "Cannot delete: this item is referenced by other records (e.g., applications).";
        }
        ra.addFlashAttribute("message", msg);

        // ако идваш от My CVs – върни към него; иначе към /home
        String referer = request.getHeader("Referer");
        if (referer != null) {
            try {
                String path = URI.create(referer).getPath();
                if (path != null && !path.isBlank()) {
                    return "redirect:" + path;
                }
            } catch (Exception ignored) {}
        }
        return "redirect:/home";
    }

    private String redirectBack(HttpServletRequest request, String fallback) {
        String referer = request.getHeader("Referer");
        try {
            if (referer != null) {
                String path = URI.create(referer).getPath();
                if (path != null && !path.isBlank()) {
                    return "redirect:" + path;
                }
            }
        } catch (Exception ignored) {}
        return "redirect:" + fallback;
    }
}