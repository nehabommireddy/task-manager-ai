package com.example.taskmanager.service;

import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate;

    @Value("${ai.anthropic.api-key}")
    private String apiKey;

    @Value("${ai.anthropic.api-url}")
    private String apiUrl;

    @Value("${ai.anthropic.model}")
    private String model;

    private static final String SYSTEM_PROMPT = """
            You are a productivity assistant helping users manage their personal tasks.
            When given a task or goal, provide a concise, actionable response.
            Format suggestions as a numbered list of clear sub-tasks or steps.
            Keep responses focused and practical — no more than 5-7 items.
            """;

    /**
     * Sends a user prompt to the Anthropic API and returns the text response.
     */
    public String getSuggestion(String userPrompt) {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.set("x-api-key", apiKey);
        headers.set("anthropic-version", "2023-06-01");

        Map<String, Object> requestBody = Map.of(
                "model", model,
                "max_tokens", 512,
                "system", SYSTEM_PROMPT,
                "messages", List.of(
                        Map.of("role", "user", "content", userPrompt)
                )
        );

        HttpEntity<Map<String, Object>> entity = new HttpEntity<>(requestBody, headers);

        log.debug("Calling Anthropic API with prompt: {}", userPrompt);

        ResponseEntity<AnthropicResponse> response = restTemplate.exchange(
                apiUrl,
                HttpMethod.POST,
                entity,
                AnthropicResponse.class
        );

        if (response.getBody() == null || response.getBody().content().isEmpty()) {
            throw new IllegalStateException("Empty response from AI API");
        }

        return response.getBody().content().get(0).text();
    }

    // ── Internal response mapping ────────────────────────────────────────────

    private record AnthropicResponse(
            List<ContentBlock> content
    ) {}

    private record ContentBlock(
            String type,
            String text
    ) {}
}
