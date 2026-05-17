package com.example.taskmanager.domain.dto;

import com.example.taskmanager.domain.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

public class TaskDtos {

    // ── Request ──────────────────────────────────────────────────────────────

    public record CreateTaskRequest(
            @NotBlank(message = "Title is required")
            @Size(max = 255, message = "Title must be 255 characters or fewer")
            String title,

            @Size(max = 2000, message = "Description must be 2000 characters or fewer")
            String description,

            Task.Status status,
            Task.Priority priority,
            LocalDate dueDate
    ) {}

    public record UpdateTaskRequest(
            @Size(max = 255, message = "Title must be 255 characters or fewer")
            String title,

            @Size(max = 2000, message = "Description must be 2000 characters or fewer")
            String description,

            Task.Status status,
            Task.Priority priority,
            LocalDate dueDate
    ) {}

    // ── Response ─────────────────────────────────────────────────────────────

    public record TaskResponse(
            Long id,
            String title,
            String description,
            Task.Status status,
            Task.Priority priority,
            LocalDate dueDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        public static TaskResponse from(Task task) {
            return new TaskResponse(
                    task.getId(),
                    task.getTitle(),
                    task.getDescription(),
                    task.getStatus(),
                    task.getPriority(),
                    task.getDueDate(),
                    task.getCreatedAt(),
                    task.getUpdatedAt()
            );
        }
    }

    // ── AI ───────────────────────────────────────────────────────────────────

    public record AiSuggestRequest(
            @NotBlank(message = "Prompt is required")
            @Size(max = 1000, message = "Prompt must be 1000 characters or fewer")
            String prompt
    ) {}

    public record AiSuggestResponse(
            String suggestion
    ) {}
}
