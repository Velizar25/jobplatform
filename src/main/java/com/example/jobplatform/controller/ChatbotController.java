package com.example.jobplatform.controller;

import com.example.jobplatform.model.ChatbotInput;
import com.example.jobplatform.service.ChatbotService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

@Controller
@RequiredArgsConstructor
public class ChatbotController {

    private final ChatbotService chatbotService;

    @GetMapping("/chatbot")
    public String form(Model model) {
        model.addAttribute("input", new ChatbotInput());

        // ✅ Greeting on open (optional, but you asked for “hi”)
        model.addAttribute("response", chatbotService.reply("hi"));

        return "chatbot";
    }

    @PostMapping("/chatbot")
    public String ask(@ModelAttribute("input") ChatbotInput input, Model model) {
        String q = (input.getMessage() == null) ? "" : input.getMessage();
        model.addAttribute("response", chatbotService.reply(q));
        return "chatbot";
    }
}