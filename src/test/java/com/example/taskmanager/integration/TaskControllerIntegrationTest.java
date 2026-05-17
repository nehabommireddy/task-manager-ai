package com.example.taskmanager.integration;

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

import java.util.Map;

import static org.hamcrest.Matchers.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
class TaskControllerIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private TaskRepository taskRepository;

    @Autowired
    private ObjectMapper objectMapper;

    @BeforeEach
    void setUp() {
        taskRepository.deleteAll();
    }

    @Test
    void GET_tasks_emptyList() throws Exception {
        mockMvc.perform(get("/api/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(0)));
    }

    @Test
    void POST_task_createsSuccessfully() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of(
                "title", "Buy groceries",
                "description", "Milk, eggs, bread",
                "priority", "HIGH"
        ));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id", notNullValue()))
                .andExpect(jsonPath("$.title", is("Buy groceries")))
                .andExpect(jsonPath("$.status", is("TODO")))
                .andExpect(jsonPath("$.priority", is("HIGH")));
    }

    @Test
    void POST_task_missingTitle_returns400() throws Exception {
        String body = objectMapper.writeValueAsString(Map.of("description", "No title here"));

        mockMvc.perform(post("/api/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.errors.title", notNullValue()));
    }

    @Test
    void GET_task_byId_returnsTask() throws Exception {
        Task saved = taskRepository.save(Task.builder()
                .title("Read a book")
                .status(Task.Status.TODO)
                .priority(Task.Priority.LOW)
                .build());

        mockMvc.perform(get("/api/tasks/" + saved.getId()))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title", is("Read a book")));
    }

    @Test
    void GET_task_notFound_returns404() throws Exception {
        mockMvc.perform(get("/api/tasks/9999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.message", containsString("9999")));
    }

    @Test
    void PATCH_task_updatesPartially() throws Exception {
        Task saved = taskRepository.save(Task.builder()
                .title("Original title")
                .status(Task.Status.TODO)
                .priority(Task.Priority.MEDIUM)
                .build());

        String patch = objectMapper.writeValueAsString(Map.of("status", "IN_PROGRESS"));

        mockMvc.perform(patch("/api/tasks/" + saved.getId())
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(patch))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.status", is("IN_PROGRESS")))
                .andExpect(jsonPath("$.title", is("Original title"))); // unchanged
    }

    @Test
    void DELETE_task_removesSuccessfully() throws Exception {
        Task saved = taskRepository.save(Task.builder()
                .title("To delete")
                .status(Task.Status.TODO)
                .priority(Task.Priority.LOW)
                .build());

        mockMvc.perform(delete("/api/tasks/" + saved.getId()))
                .andExpect(status().isNoContent());

        mockMvc.perform(get("/api/tasks/" + saved.getId()))
                .andExpect(status().isNotFound());
    }

    @Test
    void GET_tasks_filteredByStatus() throws Exception {
        taskRepository.save(Task.builder().title("Todo task")
                .status(Task.Status.TODO).priority(Task.Priority.LOW).build());
        taskRepository.save(Task.builder().title("Done task")
                .status(Task.Status.DONE).priority(Task.Priority.HIGH).build());

        mockMvc.perform(get("/api/tasks").param("status", "DONE"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$", hasSize(1)))
                .andExpect(jsonPath("$[0].title", is("Done task")));
    }
}
