package com.example.taskmanager.integration;

import java.util.Map;

import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.emptyString;
import static org.hamcrest.Matchers.greaterThanOrEqualTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.notNullValue;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@TestPropertySource(properties = "ai.gemini.api-key=")
class AiEndpointIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private ObjectMapper objectMapper;

    @Test
    void suggest_noApiKey_returns200WithFallbackStructure() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("prompt", "Plan a road trip"));

        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions").isArray())
                .andExpect(jsonPath("$.suggestions", hasSize(greaterThanOrEqualTo(1))))
                .andExpect(jsonPath("$.suggestions[0].title").isString())
                .andExpect(jsonPath("$.suggestions[0].title", not(emptyString())))
                // matches the actual first fallback reason from AiService.fallbackResponse()
                .andExpect(jsonPath("$.suggestions[0].reason",
                        containsString("Defining success upfront")));
    }

    @Test
    void suggest_noApiKey_doesNotReturnErrorEnvelope() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("prompt", "anything"));

        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status").doesNotExist())
                .andExpect(jsonPath("$.message").doesNotExist())
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    @Test
    void suggest_noApiKey_fallbackMatchesLiveResponseShape() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("prompt", "Organize a conference"));

        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.suggestions").exists())
                .andExpect(jsonPath("$.suggestions[0].title").exists())
                .andExpect(jsonPath("$.suggestions[0].reason").exists());
    }

    @Test
    void suggest_noApiKey_blankPromptStillValidates() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("prompt", ""));

        mockMvc.perform(post("/api/tasks/suggest")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.prompt", notNullValue()));
    }
}