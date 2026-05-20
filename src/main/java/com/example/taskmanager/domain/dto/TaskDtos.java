package com.example.taskmanager.domain.dto;

import com.example.taskmanager.domain.entity.Priority;
import com.example.taskmanager.domain.entity.Status;
import com.example.taskmanager.domain.entity.Task;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.time.LocalDateTime;

/**
 * All request/response records for the Task API surface live here.
 * Keeping them in one file avoids a proliferation of tiny files for a project of this size.
 * AI-specific records live in AiDtos — different domain, different file.
 */
public class TaskDtos {

    // ── Requests ─────────────────────────────────────────────────────────────

    public record CreateTaskRequest(
            @NotBlank(message = "Title is required")
            @Size(max = 255, message = "Title must be 255 characters or fewer")
            String title,

            @Size(max = 2000, message = "Description must be 2000 characters or fewer")
            String description,

            // Optional on creation — entity defaults apply when absent
            Status status,
            Priority priority,
            LocalDate dueDate
    ) {}

    public record UpdateTaskRequest(
            // All fields optional — null means "leave unchanged" (see PATCH note in TaskService)
            @Size(max = 255, message = "Title must be 255 characters or fewer")
            String title,

            @Size(max = 2000, message = "Description must be 2000 characters or fewer")
            String description,

            Status status,
            Priority priority,
            LocalDate dueDate
    ) {}

    // ── Response ─────────────────────────────────────────────────────────────

    public record TaskResponse(
            Long id,
            String title,
            String description,
            Status status,
            Priority priority,
            LocalDate dueDate,
            LocalDateTime createdAt,
            LocalDateTime updatedAt
    ) {
        // Static factory keeps mapping logic close to the response type
        // rather than scattered across service or controller layers.
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
}

