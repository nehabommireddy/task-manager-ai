package com.example.taskmanager.controller;

import com.example.taskmanager.domain.dto.AiDtos.AiSuggestResponse;
import com.example.taskmanager.domain.dto.AiDtos.TaskSuggestion;
import com.example.taskmanager.service.AiService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
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

    // ── Happy path ────────────────────────────────────────────────────────────

    @Test
    void suggest_validPrompt_returnsStructuredSuggestions() throws Exception {
        AiSuggestResponse stubResponse = new AiSuggestResponse(List.of(
                new TaskSuggestion("Book flights", "Longest lead time item."),
                new TaskSuggestion("Reserve hotel", "Rates rise closer to the date."),
                new TaskSuggestion("Pack bag", null)
        ));
        when(aiService.getSuggestions("Plan a trip")).thenReturn(stubResponse);

        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Plan a trip")))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions", hasSize(3)))
                .andExpect(jsonPath("$.suggestions[0].title", is("Book flights")))
                .andExpect(jsonPath("$.suggestions[0].reason", is("Longest lead time item.")))
                .andExpect(jsonPath("$.suggestions[1].title", is("Reserve hotel")))
                .andExpect(jsonPath("$.suggestions[2].reason").doesNotExist());
    }

    // ── Input validation ──────────────────────────────────────────────────────

    @Test
    void suggest_blankPrompt_returns400WithFieldError() throws Exception {
        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("")))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.prompt", notNullValue()))
                .andExpect(jsonPath("$.status", is(400)));
    }

    @Test
    void suggest_promptExceedsMaxLength_returns400() throws Exception {
        String oversized = "x".repeat(1001);

        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json(oversized)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.prompt", notNullValue()));
    }

    @Test
    void suggest_missingBody_returns400() throws Exception {
        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isBadRequest());
    }

    // ── AI upstream failures → 502 ────────────────────────────────────────────

    @Test
    void suggest_aiServiceThrowsIllegalState_returns502() throws Exception {
        // Throw AiServiceException with a safe user message separate from the
        // internal technical detail, so the detail cannot leak to the client
        when(aiService.getSuggestions(anyString()))
                .thenThrow(new AiService.AiServiceException(
                        "AI service failed to generate suggestions.",
                        "AI response was not valid JSON"
                ));

        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Plan a product launch")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status", is(502)))
                .andExpect(jsonPath("$.message", containsString("AI service")))
                // internal technical detail must NOT leak to the client
                .andExpect(jsonPath("$.message", not(containsString("not valid JSON"))));
    }

    @Test
    void suggest_networkFailure_returns502() throws Exception {
        when(aiService.getSuggestions(anyString()))
                .thenThrow(new RestClientException("Connection refused"));

        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("Plan a product launch")))
                .andExpect(status().isInternalServerError())
                .andExpect(jsonPath("$.status", is(500)));
    }

    // ── Response shape ────────────────────────────────────────────────────────

    @Test
    void suggest_errorResponse_hasConsistentEnvelope() throws Exception {
        when(aiService.getSuggestions(anyString()))
                .thenThrow(new IllegalStateException("empty response"));

        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(json("anything")))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.status").isNumber())
                .andExpect(jsonPath("$.message").isString())
                .andExpect(jsonPath("$.timestamp").exists())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String json(String prompt) throws Exception {
        return objectMapper.writeValueAsString(Map.of("prompt", prompt));
    }
}


