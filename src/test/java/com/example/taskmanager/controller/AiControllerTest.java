package com.example.taskmanager.controller;

import com.example.taskmanager.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Map;

import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(AiController.class)
class AiControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private AiService aiService;

    @Autowired
    private ObjectMapper objectMapper;

    @Test
    void suggest_returnsSuggestion() throws Exception {
        when(aiService.getSuggestion("Plan a trip")).thenReturn("1. Book flights\n2. Book hotel");

        String body = objectMapper.writeValueAsString(Map.of("prompt", "Plan a trip"));

        mockMvc.perform(post("/api/ai/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestion", is("1. Book flights\n2. Book hotel")));
    }

    @Test
    void suggest_blankPrompt_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("prompt", ""));

        mockMvc.perform(post("/api/ai/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }
}
