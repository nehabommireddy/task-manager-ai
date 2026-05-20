package com.example.taskmanager.integration;

import com.example.taskmanager.domain.entity.Priority;
import com.example.taskmanager.domain.entity.Status;
import com.example.taskmanager.domain.entity.Task;
import com.example.taskmanager.repository.TaskRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDate;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerIntegrationTest {

    @Autowired private MockMvc mockMvc;
    @Autowired private TaskRepository taskRepository;
    @Autowired private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    // ── GET /api/tasks ────────────────────────────────────────────────────────

    @Test
    void GET_tasks_emptyDatabase_returnsEmptyArray() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void GET_tasks_returnsAllTasks() throws Exception {
        save("Task A", Status.TODO,        Priority.LOW);
        save("Task B", Status.IN_PROGRESS, Priority.HIGH);

        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                // Both tasks present — proves findAll isn't silently filtering
                .andExpect(jsonPath("$", hasSize(2)))
                .andExpect(jsonPath("$[*].title", containsInAnyOrder("Task A", "Task B")));
    }

    @Test
    void GET_tasks_filteredByStatus_returnsOnlyMatching() throws Exception {
        save("Todo task", Status.TODO, Priority.LOW);
        save("Done task", Status.DONE, Priority.HIGH);

        mockMvc.perform(get("/api/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                // Size assertion proves the non-matching task was excluded, not just that one matched
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Done task")))
                .andExpect(jsonPath("$[0].status", is("DONE")));
    }

    @Test
    void GET_tasks_filteredByPriority_returnsOnlyMatching() throws Exception {
        save("Low priority",  Status.TODO, Priority.LOW);
        save("High priority", Status.TODO, Priority.HIGH);

        mockMvc.perform(get("/api/tasks").param("priority", "HIGH"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].priority", is("HIGH")));
    }

    @Test
    void GET_tasks_filteredByStatusAndPriority_returnsOnlyMatching() throws Exception {
        save("Match",           Status.TODO, Priority.HIGH);
        save("Wrong priority",  Status.TODO, Priority.LOW);
        save("Wrong status",    Status.DONE, Priority.HIGH);

        mockMvc.perform(get("/api/tasks")
                        .param("status",   "TODO")
                        .param("priority", "HIGH"))
                .andExpect(status().isOk())
                // Only "Match" satisfies both conditions
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Match")));
    }

    // ── GET /api/tasks/{id} ───────────────────────────────────────────────────

    @Test
    void GET_task_byId_returnsFullPayload() throws Exception {
        Task saved = taskRepository.save(Task.builder()
                .title("Read a book")
                .description("Finish Clean Code")
                .status(Status.IN_PROGRESS)
                .priority(Priority.MEDIUM)
                .dueDate(LocalDate.of(2025, 12, 31))
                .build());

        mockMvc.perform(get("/api/tasks/" + saved.getId()))
                .andExpect(status().isOk())
                // Assert every field in the response envelope, not just title
                .andExpect(jsonPath("$.id",          is(saved.getId().intValue())))
                .andExpect(jsonPath("$.title",       is("Read a book")))
                .andExpect(jsonPath("$.description", is("Finish Clean Code")))
                .andExpect(jsonPath("$.status",      is("IN_PROGRESS")))
                .andExpect(jsonPath("$.priority",    is("MEDIUM")))
                .andExpect(jsonPath("$.dueDate",     is("2025-12-31")))
                .andExpect(jsonPath("$.createdAt",   notNullValue()))
                .andExpect(jsonPath("$.updatedAt",   notNullValue()));
    }

    @Test
    void GET_task_notFound_returnsErrorEnvelope() throws Exception {
        mockMvc.perform(get("/api/tasks/9999"))
                .andExpect(status().isNotFound())
                // Verify the full error envelope shape, not just the message
                .andExpect(jsonPath("$.status",    is(404)))
                .andExpect(jsonPath("$.message",   containsString("9999")))
                .andExpect(jsonPath("$.timestamp", notNullValue()))
                // 'errors' field should be absent for non-validation failures
                .andExpect(jsonPath("$.errors").doesNotExist());
    }

    // ── POST /api/tasks ───────────────────────────────────────────────────────

    @Test
    void POST_task_fullRequest_persistsAllFields() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title",       "Plan sprint",
                "description", "Q4 planning session",
                "status",      "IN_PROGRESS",
                "priority",    "HIGH",
                "dueDate",     "2025-11-01"
        ));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id",          notNullValue()))
                .andExpect(jsonPath("$.title",       is("Plan sprint")))
                .andExpect(jsonPath("$.description", is("Q4 planning session")))
                .andExpect(jsonPath("$.status",      is("IN_PROGRESS")))
                .andExpect(jsonPath("$.priority",    is("HIGH")))
                .andExpect(jsonPath("$.dueDate",     is("2025-11-01")))
                .andExpect(jsonPath("$.createdAt",   notNullValue()))
                .andExpect(jsonPath("$.updatedAt",   notNullValue()));
    }

    @Test
    void POST_task_titleOnly_appliesEntityDefaults() throws Exception {
        // When optional fields are omitted, entity @Builder.Default values must apply
        String body = objectMapper.writeValueAsString(Map.of("title", "Minimal task"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.title",       is("Minimal task")))
                .andExpect(jsonPath("$.status",      is("TODO")))    // @Builder.Default
                .andExpect(jsonPath("$.priority",    is("MEDIUM")))  // @Builder.Default
                .andExpect(jsonPath("$.description").doesNotExist())
                .andExpect(jsonPath("$.dueDate").doesNotExist());
    }

    @Test
    void POST_task_returnsLocationHeader() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", "Check Location header"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                // Location header must be present and point to the new resource
                .andExpect(header().exists("Location"))
                .andExpect(header().string("Location", containsString("/api/tasks/")));
    }

    @Test
    void POST_task_missingTitle_returns400WithFieldError() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("description", "No title here"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.status",       is(400)))
                .andExpect(jsonPath("$.message",      is("Validation failed")))
                .andExpect(jsonPath("$.errors.title", notNullValue()))
                .andExpect(jsonPath("$.timestamp",    notNullValue()));
    }

    @Test
    void POST_task_blankTitle_returns400() throws Exception {
        // @NotBlank rejects whitespace-only strings — not covered by @Size alone
        String body = objectMapper.writeValueAsString(Map.of("title", "   "));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title", notNullValue()));
    }

    // ── PUT /api/tasks/{id} ───────────────────────────────────────────────────

    @Test
    void PUT_task_replacesAllFields() throws Exception {
        Task saved = taskRepository.save(Task.builder()
                .title("Original")
                .description("Original desc")
                .status(Status.DONE)
                .priority(Priority.HIGH)
                .build());

        // PUT with only a title — all other fields should reset to defaults
        String body = objectMapper.writeValueAsString(Map.of("title", "Replaced title"));

        mockMvc.perform(put("/api/tasks/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title",    is("Replaced title")))
                // These reset to entity defaults — not left at their previous values
                .andExpect(jsonPath("$.status",   is("TODO")))
                .andExpect(jsonPath("$.priority", is("MEDIUM")));
    }

    @Test
    void PUT_task_notFound_returns404() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("title", "Doesn't matter"));

        mockMvc.perform(put("/api/tasks/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status", is(404)));
    }

    // ── PATCH /api/tasks/{id} ─────────────────────────────────────────────────

    @Test
    void PATCH_task_singleField_leavesOthersUnchanged() throws Exception {
        Task saved = taskRepository.save(Task.builder()
                .title("Original title")
                .status(Status.TODO)
                .priority(Priority.MEDIUM)
                .build());

        String patch = objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"));

        mockMvc.perform(patch("/api/tasks/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status",   is("IN_PROGRESS")))  // changed
                .andExpect(jsonPath("$.title",    is("Original title")))  // unchanged
                .andExpect(jsonPath("$.priority", is("MEDIUM")));      // unchanged
    }

    @Test
    void PATCH_task_updatesTimestamp() throws Exception {
        Task saved = taskRepository.save(Task.builder()
                .title("Timestamped task")
                .status(Status.TODO)
                .priority(Priority.LOW)
                .build());

        String savedUpdatedAt = saved.getUpdatedAt().toString();

        // Small sleep ensures updatedAt will differ if @UpdateTimestamp fires correctly
        Thread.sleep(10);

        String patch = objectMapper.writeValueAsString(Map.of("status", "DONE"));

        mockMvc.perform(patch("/api/tasks/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk())
                // updatedAt must change on every write — verifies @UpdateTimestamp works
                .andExpect(jsonPath("$.updatedAt", not(savedUpdatedAt)));
    }

    @Test
    void PATCH_task_notFound_returns404() throws Exception {
        String patch = objectMapper.writeValueAsString(Map.of("status", "DONE"));

        mockMvc.perform(patch("/api/tasks/9999")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status",  is(404)))
                .andExpect(jsonPath("$.message", containsString("9999")));
    }

    // ── DELETE /api/tasks/{id} ────────────────────────────────────────────────

    @Test
    void DELETE_task_removesFromDatabase() throws Exception {
        Task saved = save("To delete", Status.TODO, Priority.LOW);

        mockMvc.perform(delete("/api/tasks/" + saved.getId()))
                .andExpect(status().isNoContent());

        // Verify at the database level, not just via another HTTP call
        assertThat(taskRepository.existsById(saved.getId())).isFalse();
    }

    @Test
    void DELETE_task_notFound_returns404() throws Exception {
        mockMvc.perform(delete("/api/tasks/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status",  is(404)))
                .andExpect(jsonPath("$.message", containsString("9999")));
    }

    @Test
    void DELETE_task_isIdempotentAtHttpLevel() throws Exception {
        // Second delete on the same ID returns 404, not 500 — important for retry safety
        Task saved = save("Delete me twice", Status.TODO, Priority.LOW);
        Long id = saved.getId();

        mockMvc.perform(delete("/api/tasks/" + id))
                .andExpect(status().isNoContent());

        mockMvc.perform(delete("/api/tasks/" + id))
                .andExpect(status().isNotFound());  // not 500
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private Task save(String title, Status status, Priority priority) {
        return taskRepository.save(Task.builder()
                .title(title)
                .status(status)
                .priority(priority)
                .build());
    }
}

