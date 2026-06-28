package com.example.jobplatform.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
public class HomeController {

    @GetMapping("/")
    public Map<String, String> home() {
        return Map.of(
                "message", "Job Platform REST API is running",
                "frontend", "React application",
                "backend", "Spring Boot"
        );
    }

    @GetMapping("/api")
    public Map<String, String> api() {
        return Map.of(
                "message", "Welcome to Job Platform API",
                "status", "running"
        );
    }
}