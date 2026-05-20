package com.example.taskmanager.domain.dto;

import java.util.List;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Request/response records for the AI suggestion endpoint.
 * Kept separate from TaskDtos — different domain concern, different file.
 */
public class AiDtos {

    public record AiSuggestRequest(
            @NotBlank(message = "Prompt is required")
            @Size(max = 1000, message = "Prompt must be 1000 characters or fewer")
            String prompt
    ) {}

    /**
     * Structured response returned to the client.
     * Each suggestion is a discrete task item with a title and optional reason,
     * so the client can display, filter, or import them individually without
     * parsing a raw text blob.
     */
    public record AiSuggestResponse(
            List<TaskSuggestion> suggestions
    ) {}

    /**
     * A single suggested sub-task.
     *
     * @param title  Short, actionable task title — suitable for direct import as a Task.
     * @param reason Why this step matters in context of the goal. May be null if the
     *               model omits it; clients should handle absence gracefully.
     */
    public record TaskSuggestion(
            String title,
            String reason
    ) {}
}
