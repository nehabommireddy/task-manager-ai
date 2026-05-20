package com.example.taskmanager.service;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Service;
import org.springframework.web.client.HttpClientErrorException;
import org.springframework.web.client.HttpServerErrorException;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import com.example.taskmanager.domain.dto.AiDtos.AiSuggestResponse;
import com.example.taskmanager.domain.dto.AiDtos.TaskSuggestion;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Service
@RequiredArgsConstructor
public class AiService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${ai.gemini.api-key:}")
    private String apiKey;

    @Value("${ai.gemini.api-url}")
    private String apiUrl;

    private static final String SYSTEM_PROMPT = """
            You are a productivity assistant that breaks goals into actionable tasks.

            Respond ONLY with valid JSON:
            {
              "suggestions": [
                {
                  "title": "Actionable task",
                  "reason": "One sentence explanation"
                }
              ]
            }

            Rules:
            - 3–5 suggestions
            - Titles start with verbs
            - No markdown, no extra text
            """;

    // ─────────────────────────────────────────────────────────────
    // EXCEPTION
    // ─────────────────────────────────────────────────────────────

    public static class AiServiceException extends RuntimeException {
        private final String userMessage;

        public AiServiceException(String userMessage, String technicalDetail, Throwable cause) {
            super(technicalDetail, cause);
            this.userMessage = userMessage;
        }

        public AiServiceException(String userMessage, String technicalDetail) {
            super(technicalDetail);
            this.userMessage = userMessage;
        }

        public String getUserMessage() {
            return userMessage;
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PUBLIC API
    // ─────────────────────────────────────────────────────────────

    public AiSuggestResponse getSuggestions(String userPrompt) {
        if (apiKey == null || apiKey.isBlank()) {
            log.warn("⚠️ GEMINI_API_KEY is missing — using fallback response");
            return fallbackResponse();
        }

        try {
            String raw = callApi(userPrompt);
            return parseResponse(raw);

        } catch (AiServiceException e) {
            // quota / transient errors → return fallback instead of crashing
            if (e.getMessage() != null && e.getMessage().contains("QUOTA")) {
                log.warn("⚠️ Quota exceeded — serving fallback suggestions");
                return fallbackResponse();
            }
            throw e;

        } catch (Exception e) {
            log.error("❌ Unexpected AI pipeline failure (prompt={})", userPrompt, e);
            throw new AiServiceException(
                    "Something went wrong while generating suggestions. Please try again.",
                    "Unexpected error: " + e.getMessage(),
                    e
            );
        }
    }

    // ─────────────────────────────────────────────────────────────
    // API CALL
    // ─────────────────────────────────────────────────────────────

    private String callApi(String userPrompt) {

        String url = UriComponentsBuilder.fromUriString(apiUrl)
                .queryParam("key", apiKey)
                .toUriString();

        Map<String, Object> body = Map.of(
                "systemInstruction", Map.of(
                        "parts", List.of(Map.of("text", SYSTEM_PROMPT))
                ),
                "contents", List.of(Map.of(
                        "role", "user",
                        "parts", List.of(Map.of("text", userPrompt))
                )),
                "generationConfig", Map.of(
                        "maxOutputTokens", 1024,
                        "temperature", 0.4
                )
        );

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);

        log.info("📡 Calling Gemini API...");

        ResponseEntity<GeminiResponse> response;

        try {
            response = restTemplate.exchange(
                    url,
                    HttpMethod.POST,
                    new HttpEntity<>(body, headers),
                    GeminiResponse.class
            );

        } catch (HttpClientErrorException e) {
            HttpStatus status = HttpStatus.resolve(e.getStatusCode().value());
            log.error("❌ Gemini client error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServiceException(
                    toUserMessage(status),
                    "QUOTA:HTTP " + e.getStatusCode() + " — " + e.getResponseBodyAsString(),
                    e
            );

        } catch (HttpServerErrorException e) {
            log.error("❌ Gemini server error: {} {}", e.getStatusCode(), e.getResponseBodyAsString());
            throw new AiServiceException(
                    "The AI service is temporarily unavailable. Please try again in a moment.",
                    "HTTP " + e.getStatusCode() + " — " + e.getResponseBodyAsString(),
                    e
            );

        } catch (ResourceAccessException e) {
            log.error("❌ Network error reaching Gemini: {}", e.getMessage());
            throw new AiServiceException(
                    "Could not reach the AI service. Please check your connection and try again.",
                    "Network error: " + e.getMessage(),
                    e
            );

        } catch (Exception e) {
            log.error("❌ Unexpected error calling Gemini: {}", e.getMessage(), e);
            throw new AiServiceException(
                    "Something went wrong while contacting the AI service. Please try again.",
                    "Unexpected call error: " + e.getMessage(),
                    e
            );
        }

        GeminiResponse bodyResponse = response.getBody();

        if (bodyResponse == null
                || bodyResponse.candidates() == null
                || bodyResponse.candidates().isEmpty()) {
            throw new AiServiceException(
                    "The AI service didn't return any suggestions. Please try again.",
                    "Gemini returned empty candidates"
            );
        }

        try {
            String text = bodyResponse
                    .candidates().get(0)
                    .content().parts().get(0)
                    .text();

            if (text == null || text.isBlank()) {
                throw new AiServiceException(
                        "The AI service returned an empty response. Please try again.",
                        "Gemini returned blank text"
                );
            }

            log.debug("🧠 Raw AI output: {}", abbreviated(text));
            return text;

        } catch (AiServiceException e) {
            throw e;
        } catch (Exception e) {
            log.error("❌ Failed to extract text from Gemini response", e);
            throw new AiServiceException(
                    "Received an unexpected response from the AI service.",
                    "Invalid response structure: " + e.getMessage(),
                    e
            );
        }
    }

    // ─────────────────────────────────────────────────────────────
    // PARSING
    // ─────────────────────────────────────────────────────────────

    AiSuggestResponse parseResponse(String rawText) {

        String cleaned = stripMarkdownFences(rawText.strip());

        log.info("🔍 Parsing AI response...");
        log.debug("Cleaned response: {}", abbreviated(cleaned));

        try {
            JsonNode root = objectMapper.readTree(cleaned);

            if (!root.has("suggestions") || !root.get("suggestions").isArray()) {
                throw new AiServiceException(
                        "The AI returned an unexpected format. Please try again.",
                        "Missing 'suggestions' array in: " + abbreviated(cleaned)
                );
            }

            List<TaskSuggestion> suggestions = new ArrayList<>();

            for (JsonNode item : root.get("suggestions")) {
                String title = item.path("title").asText(null);
                if (title == null || title.isBlank()) {
                    log.warn("Skipping invalid suggestion (missing title): {}", item);
                    continue;
                }
                suggestions.add(new TaskSuggestion(title.strip(), item.path("reason").asText(null)));
            }

            if (suggestions.isEmpty()) {
                throw new AiServiceException(
                        "The AI couldn't generate any suggestions for that input. Try rephrasing your goal.",
                        "No valid suggestions parsed from: " + abbreviated(cleaned)
                );
            }

            return new AiSuggestResponse(suggestions);

        } catch (AiServiceException e) {
            throw e;
        } catch (JsonProcessingException e) {
            log.error("❌ AI returned invalid JSON: {}", abbreviated(cleaned));
            throw new AiServiceException(
                    "The AI returned an unreadable response. Please try again.",
                    "Invalid JSON: " + e.getOriginalMessage(),
                    e
            );
        }
    }

    // ─────────────────────────────────────────────────────────────
    // USER MESSAGE MAPPING
    // ─────────────────────────────────────────────────────────────

    private String toUserMessage(HttpStatus status) {
        if (status == null) {
            return "An unexpected error occurred. Please try again.";
        }
        return switch (status) {
            case TOO_MANY_REQUESTS -> "AI suggestions are temporarily unavailable due to high demand. Showing default suggestions instead.";
            case UNAUTHORIZED, FORBIDDEN -> "The AI service is not properly configured. Please contact support.";
            case NOT_FOUND -> "The AI model could not be found. Please contact support.";
            case BAD_REQUEST -> "The request to the AI service was invalid. Please try again.";
            default -> "The AI service returned an error. Please try again later.";
        };
    }

    // ─────────────────────────────────────────────────────────────
    // FALLBACK
    // ─────────────────────────────────────────────────────────────

    private AiSuggestResponse fallbackResponse() {
        return new AiSuggestResponse(List.of(
                new TaskSuggestion(
                        "Set a clear goal and deadline",
                        "Defining success upfront keeps you focused and accountable."
                ),
                new TaskSuggestion(
                        "Break goal into milestones",
                        "Smaller steps make progress measurable and easier to track."
                ),
                new TaskSuggestion(
                        "Pick one priority task today",
                        "Focus prevents overload and increases execution speed."
                )
        ));
    }

    // ─────────────────────────────────────────────────────────────
    // HELPERS
    // ─────────────────────────────────────────────────────────────

    private String stripMarkdownFences(String text) {
        if (text.startsWith("```")) {
            int firstNewline = text.indexOf('\n');
            int lastFence = text.lastIndexOf("```");
            if (firstNewline != -1 && lastFence > firstNewline) {
                return text.substring(firstNewline + 1, lastFence).strip();
            }
        }
        return text;
    }

    private String abbreviated(String text) {
        return text.length() > 250 ? text.substring(0, 250) + "..." : text;
    }

    // ─────────────────────────────────────────────────────────────
    // GEMINI RESPONSE MODEL
    // ─────────────────────────────────────────────────────────────

    private record GeminiResponse(List<Candidate> candidates) {}
    private record Candidate(Content content) {}
    private record Content(List<Part> parts) {}
    private record Part(String text) {}
}