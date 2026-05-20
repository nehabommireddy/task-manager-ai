package com.example.taskmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.example.taskmanager.domain.dto.AiDtos.AiSuggestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

class AiServiceParsingTest {

    private AiService aiService;

    @BeforeEach
    void setUp() {
        aiService = new AiService(null, new ObjectMapper());
    }

    // ---------------- Happy path ----------------

    @Test
    void parseResponse_validJson_returnsSuggestions() {
        String json = """
                {
                  "suggestions": [
                    { "title": "Book flights", "reason": "Longest lead time." },
                    { "title": "Reserve hotel", "reason": "Rates rise closer to date." }
                  ]
                }
                """;

        AiSuggestResponse res = aiService.parseResponse(json);

        assertThat(res.suggestions()).hasSize(2);
    }

    @Test
    void parseResponse_reasonOptional_works() {
        String json = """
                {
                  "suggestions": [
                    { "title": "Buy groceries" }
                  ]
                }
                """;

        AiSuggestResponse res = aiService.parseResponse(json);

        assertThat(res.suggestions()).hasSize(1);
        assertThat(res.suggestions().get(0).reason()).isNull();
    }

    // ---------------- Fence handling ----------------

    @Test
    void parseResponse_fencedJson_parses() {
        String fenced = """
                ```json
                { "suggestions": [{ "title": "Task", "reason": "Reason" }] }
                ```
                """;

        AiSuggestResponse res = aiService.parseResponse(fenced);

        assertThat(res.suggestions()).hasSize(1);
    }

    // ---------------- Partial failure ----------------

    @Test
    void parseResponse_skipsMissingTitles() {
        String json = """
                {
                  "suggestions": [
                    { "title": "Valid", "reason": "ok" },
                    { "reason": "invalid" }
                  ]
                }
                """;

        AiSuggestResponse res = aiService.parseResponse(json);

        assertThat(res.suggestions()).hasSize(1);
    }

    // ---------------- Hard failures (UPDATED) ----------------

    @Test
    void parseResponse_notJson_throws() {
        assertThatThrownBy(() -> aiService.parseResponse("Hello world"))
                .isInstanceOf(AiService.AiServiceException.class)
                .hasMessageContaining("Invalid JSON");
    }

    @Test
    void parseResponse_missingField_throws() {
        String json = "{ \"tasks\": [] }";

        assertThatThrownBy(() -> aiService.parseResponse(json))
                .isInstanceOf(AiService.AiServiceException.class)
                .hasMessageContaining("Missing 'suggestions' array");
    }

    @Test
    void parseResponse_emptyArray_throws() {
        String json = "{ \"suggestions\": [] }";

        assertThatThrownBy(() -> aiService.parseResponse(json))
                .isInstanceOf(AiService.AiServiceException.class)
                .hasMessageContaining("No valid suggestions parsed");
    }

    @Test
    void parseResponse_allInvalidItems_throws() {
        String json = """
                {
                  "suggestions": [
                    { "reason": "no title" }
                  ]
                }
                """;

        assertThatThrownBy(() -> aiService.parseResponse(json))
                .isInstanceOf(AiService.AiServiceException.class)
                .hasMessageContaining("No valid suggestions parsed");
    }
}
