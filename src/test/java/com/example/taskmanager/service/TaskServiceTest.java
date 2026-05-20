package com.example.taskmanager.service;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import static org.mockito.ArgumentMatchers.any;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;

import com.example.taskmanager.domain.dto.TaskDtos.CreateTaskRequest;
import com.example.taskmanager.domain.dto.TaskDtos.TaskResponse;
import com.example.taskmanager.domain.dto.TaskDtos.UpdateTaskRequest;
import com.example.taskmanager.domain.entity.Priority;
import com.example.taskmanager.domain.entity.Status;
import com.example.taskmanager.domain.entity.Task;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.repository.TaskRepository;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getAllTasks_returnsAllTasks() {
        Task task = buildTask(1L, "Test task", Status.TODO);
        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<TaskResponse> result = taskService.getAllTasks(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Test task");
    }

    @Test
    void getAllTasks_filteredByStatus() {
        Task task = buildTask(1L, "In progress task", Status.IN_PROGRESS);
        when(taskRepository.findByStatus(Status.IN_PROGRESS)).thenReturn(List.of(task));

        List<TaskResponse> result = taskService.getAllTasks(Status.IN_PROGRESS, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(Status.IN_PROGRESS);
    }

    @Test
    void getTaskById_existingId_returnsTask() {
        Task task = buildTask(1L, "Found task", Status.TODO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        TaskResponse result = taskService.getTaskById(1L);

        assertThat(result.id()).isEqualTo(1L);
        assertThat(result.title()).isEqualTo("Found task");
    }

    @Test
    void getTaskById_missingId_throwsNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.getTaskById(99L))
                .isInstanceOf(TaskNotFoundException.class)
                .hasMessageContaining("99");
    }

    @Test
    void createTask_savesAndReturns() {
        CreateTaskRequest request = new CreateTaskRequest(
                "New task", "Do the thing", null, null, null
        );
        Task saved = buildTask(1L, "New task", Status.TODO);
        when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(saved);

        TaskResponse result = taskService.createTask(request);

        assertThat(result.title()).isEqualTo("New task");
        verify(taskRepository, times(1)).saveAndFlush(any(Task.class));
    }

    @Test
    void updateTask_partialUpdate_onlyChangesProvidedFields() {
        Task existing = buildTask(1L, "Old title", Status.TODO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(existing);

        UpdateTaskRequest request = new UpdateTaskRequest(
                "New title", null, Status.IN_PROGRESS, null, null
        );
        taskService.updateTask(1L, request);

        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getStatus()).isEqualTo(Status.IN_PROGRESS);
        assertThat(existing.getPriority()).isEqualTo(Priority.MEDIUM); // unchanged
    }

    @Test
    void replaceTask_overwritesAllFields() {
        Task existing = buildTask(1L, "Old title", Status.DONE);
        existing.setPriority(Priority.HIGH);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.saveAndFlush(any(Task.class))).thenReturn(existing);

        // PUT: all fields replaced — no status provided so it resets to TODO
        CreateTaskRequest request = new CreateTaskRequest(
                "New title", "New desc", null, null, null
        );
        taskService.replaceTask(1L, request);

        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getDescription()).isEqualTo("New desc");
        assertThat(existing.getStatus()).isEqualTo(Status.TODO);       // reset to default
        assertThat(existing.getPriority()).isEqualTo(Priority.MEDIUM); // reset to default
    }

    @Test
    void deleteTask_existingId_deletesSuccessfully() {
        Task task = buildTask(1L, "To delete", Status.TODO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatCode(() -> taskService.deleteTask(1L)).doesNotThrowAnyException();
        verify(taskRepository).delete(task);
    }

    @Test
    void deleteTask_missingId_throwsNotFound() {
        when(taskRepository.findById(99L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Task buildTask(Long id, String title, Status status) {
        return Task.builder()
                .id(id)
                .title(title)
                .status(status)
                .priority(Priority.MEDIUM)
                .build();
    }
}

