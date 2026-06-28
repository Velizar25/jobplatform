package com.example.jobplatform.controller;

import com.example.jobplatform.service.ChatbotService;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/chatbot")
public class ChatbotRestController {

    private final ChatbotService chatbotService;

    public ChatbotRestController(ChatbotService chatbotService) {
        this.chatbotService = chatbotService;
    }

    @GetMapping("/welcome")
    public ChatbotResponse welcome() {
        return new ChatbotResponse(chatbotService.reply("hi"));
    }

    @PostMapping("/ask")
    public ChatbotResponse ask(@RequestBody ChatbotRequest request) {
        String message = request.message() == null ? "" : request.message();
        return new ChatbotResponse(chatbotService.reply(message));
    }

    public record ChatbotRequest(String message) {
    }

    public record ChatbotResponse(String response) {
    }
}