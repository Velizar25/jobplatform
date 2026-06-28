package com.example.jobplatform.service;

import com.example.jobplatform.repository.JobRepository;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ChatbotServiceTest {

    @Test
    void shouldReplyToGreeting() {
        JobRepository jobRepository = mock(JobRepository.class);
        ChatbotService chatbotService = new ChatbotService(jobRepository);

        String response = chatbotService.reply("hello");

        assertTrue(response.contains("JobPlatform assistant"));
    }

    @Test
    void shouldReturnNoJobsMessageWhenNoJobsExist() {
        JobRepository jobRepository = mock(JobRepository.class);
        when(jobRepository.findAllByDeletedAtIsNull()).thenReturn(List.of());

        ChatbotService chatbotService = new ChatbotService(jobRepository);

        String response = chatbotService.reply("recommend jobs");

        assertTrue(response.contains("no active job offers"));
    }
}