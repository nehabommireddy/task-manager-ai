package com.example.taskmanager.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpMethod;
import org.springframework.http.ResponseEntity;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;

import com.example.taskmanager.domain.dto.AiDtos.AiSuggestResponse;
import com.fasterxml.jackson.databind.ObjectMapper;

@ExtendWith(MockitoExtension.class)
class AiServiceHttpTest {

    @Mock
    private RestTemplate restTemplate;

    private AiService aiService;
    private final ObjectMapper objectMapper = new ObjectMapper();

    @BeforeEach
    void setUp() {
        aiService = new AiService(restTemplate, objectMapper);
        ReflectionTestUtils.setField(aiService, "apiKey", "test-gemini-key");
        ReflectionTestUtils.setField(aiService, "apiUrl",
                "https://generativelanguage.googleapis.com/v1beta/models/gemini-1.5-flash:generateContent");
    }

    // ---------------- Happy path ----------------

    @Test
    void getSuggestions_validApiResponse_returnsStructuredResult() {
        stubWithModelText(validSuggestionsJson());

        AiSuggestResponse response = aiService.getSuggestions("Plan a product launch");

        assertThat(response.suggestions()).hasSize(2);
        assertThat(response.suggestions().get(0).title()).isEqualTo("Define target audience");
        assertThat(response.suggestions().get(0).reason())
                .isEqualTo("Knowing your audience shapes every other decision.");
    }

    @Test
    void getSuggestions_callsApiExactlyOnce() {
        stubWithModelText(validSuggestionsJson());

        aiService.getSuggestions("any prompt");

        verify(restTemplate, times(1))
                .exchange(anyString(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class));
    }

    @Test
    void getSuggestions_sendsPromptInRequestBody() throws Exception {
        stubWithModelText(validSuggestionsJson());

        ArgumentCaptor<HttpEntity> captor = ArgumentCaptor.forClass(HttpEntity.class);

        aiService.getSuggestions("My specific prompt");

        verify(restTemplate).exchange(anyString(), eq(HttpMethod.POST), captor.capture(), any(Class.class));

        String body = objectMapper.writeValueAsString(captor.getValue().getBody());
        assertThat(body).contains("My specific prompt");
    }

    @Test
    void getSuggestions_sendsApiKeyAsQueryParam() {
        stubWithModelText(validSuggestionsJson());

        ArgumentCaptor<String> urlCaptor = ArgumentCaptor.forClass(String.class);

        aiService.getSuggestions("any prompt");

        verify(restTemplate).exchange(urlCaptor.capture(), eq(HttpMethod.POST), any(HttpEntity.class), any(Class.class));

        assertThat(urlCaptor.getValue()).contains("key=test-gemini-key");
    }

    // ---------------- Fallback ----------------

    @Test
    void getSuggestions_noApiKey_returnsFallbackWithoutHttpCall() {
        ReflectionTestUtils.setField(aiService, "apiKey", "");

        AiSuggestResponse response = aiService.getSuggestions("any");

        assertThat(response.suggestions()).isNotEmpty();
        verifyNoInteractions(restTemplate);
    }

    @Test
    void getSuggestions_nullApiKey_returnsFallbackWithoutHttpCall() {
        ReflectionTestUtils.setField(aiService, "apiKey", null);

        AiSuggestResponse response = aiService.getSuggestions("any");

        assertThat(response.suggestions()).isNotEmpty();
        verifyNoInteractions(restTemplate);
    }

    // ---------------- Error cases (UPDATED) ----------------

    @Test
    void getSuggestions_nullBody_throwsAiServiceException() {
        when(restTemplate.exchange(anyString(), any(), any(), any(Class.class)))
                .thenReturn(ResponseEntity.ok(null));

        assertThatThrownBy(() -> aiService.getSuggestions("any"))
                .isInstanceOf(AiService.AiServiceException.class)
                .hasMessageContaining("Gemini returned empty candidates");
    }

    @Test
    void getSuggestions_modelIgnoredJsonPrompt_throwsAiServiceException() {
        stubWithModelText("Here are your tasks");

        assertThatThrownBy(() -> aiService.getSuggestions("any"))
                .isInstanceOf(AiService.AiServiceException.class)
                .hasMessageContaining("Invalid JSON");
    }

    @Test
    void getSuggestions_modelReturnedWrongSchema_throwsAiServiceException() {
        stubWithModelText("{ \"items\": [] }");

        assertThatThrownBy(() -> aiService.getSuggestions("any"))
                .isInstanceOf(AiService.AiServiceException.class)
                .hasMessageContaining("Missing 'suggestions' array");
    }

    @Test
    void getSuggestions_networkTimeout_wrapsException() {
        when(restTemplate.exchange(anyString(), any(), any(), any(Class.class)))
                .thenThrow(new ResourceAccessException("Read timed out"));

        assertThatThrownBy(() -> aiService.getSuggestions("any"))
                .isInstanceOf(AiService.AiServiceException.class)
                .hasMessageContaining("Network error");
    }

    // ---------------- Helpers ----------------

    private void stubWithModelText(String modelText) {
        try {
            var part = java.util.Map.of("text", modelText);
            var content = java.util.Map.of("parts", java.util.List.of(part));
            var candidate = java.util.Map.of("content", content);
            var envelope = java.util.Map.of("candidates", java.util.List.of(candidate));

            when(restTemplate.exchange(anyString(), any(), any(), any(Class.class)))
                    .thenAnswer(inv -> {
                        Class<?> type = inv.getArgument(3);
                        String json = objectMapper.writeValueAsString(envelope);
                        Object parsed = objectMapper.readValue(json, type);
                        return ResponseEntity.ok(parsed);
                    });
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }

    private String validSuggestionsJson() {
        return """
                {
                  "suggestions": [
                    { "title": "Define target audience", "reason": "Knowing your audience shapes every other decision." },
                    { "title": "Set a launch date", "reason": "A fixed date creates urgency across all teams." }
                  ]
                }
                """;
    }
}
