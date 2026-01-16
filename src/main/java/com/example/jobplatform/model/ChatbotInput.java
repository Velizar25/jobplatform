package com.example.jobplatform.model;

public class ChatbotInput {
    private String message;

    public ChatbotInput() {}

    public ChatbotInput(String message) {
        this.message = message;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
