package com.example.jobplatform.service;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class ChatbotServiceTest {

    private final ChatbotService chatbot = new ChatbotService();

    @Test
    void greeting_returnsHelloMessage() {
        String response = chatbot.reply("hi");

        assertThat(response.toLowerCase()).contains("hi");
    }

    @Test
    void applyQuestion_returnsSteps() {
        String response = chatbot.reply("how do I apply for a job");

        assertThat(response.toLowerCase()).contains("apply");
        assertThat(response).contains("Jobs");
    }

    @Test
    void uploadCvQuestion_returnsInstructions() {
        String response = chatbot.reply("upload cv");

        assertThat(response.toLowerCase()).contains("cv");
        assertThat(response).contains("PDF");
    }

    @Test
    void unknownQuestion_returnsFallbackHelp() {
        String response = chatbot.reply("asdasdasd");

        assertThat(response.toLowerCase()).contains("i can help");
    }
}