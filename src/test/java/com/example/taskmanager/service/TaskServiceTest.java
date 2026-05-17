package com.example.taskmanager.service;

import com.example.taskmanager.domain.dto.TaskDtos.*;
import com.example.taskmanager.domain.entity.Task;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService taskService;

    @Test
    void getAllTasks_returnsAllTasks() {
        Task task = buildTask(1L, "Test task", Task.Status.TODO);
        when(taskRepository.findAll()).thenReturn(List.of(task));

        List<TaskResponse> result = taskService.getAllTasks(null, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).title()).isEqualTo("Test task");
    }

    @Test
    void getAllTasks_filteredByStatus() {
        Task task = buildTask(1L, "In progress task", Task.Status.IN_PROGRESS);
        when(taskRepository.findByStatus(Task.Status.IN_PROGRESS)).thenReturn(List.of(task));

        List<TaskResponse> result = taskService.getAllTasks(Task.Status.IN_PROGRESS, null);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).status()).isEqualTo(Task.Status.IN_PROGRESS);
    }

    @Test
    void getTaskById_existingId_returnsTask() {
        Task task = buildTask(1L, "Found task", Task.Status.TODO);
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
        Task saved = buildTask(1L, "New task", Task.Status.TODO);
        when(taskRepository.save(any(Task.class))).thenReturn(saved);

        TaskResponse result = taskService.createTask(request);

        assertThat(result.title()).isEqualTo("New task");
        verify(taskRepository, times(1)).save(any(Task.class));
    }

    @Test
    void updateTask_partialUpdate_onlyChangesProvidedFields() {
        Task existing = buildTask(1L, "Old title", Task.Status.TODO);
        when(taskRepository.findById(1L)).thenReturn(Optional.of(existing));
        when(taskRepository.save(any(Task.class))).thenReturn(existing);

        UpdateTaskRequest request = new UpdateTaskRequest(
                "New title", null, Task.Status.IN_PROGRESS, null, null
        );
        taskService.updateTask(1L, request);

        assertThat(existing.getTitle()).isEqualTo("New title");
        assertThat(existing.getStatus()).isEqualTo(Task.Status.IN_PROGRESS);
        assertThat(existing.getPriority()).isEqualTo(Task.Priority.MEDIUM); // unchanged
    }

    @Test
    void deleteTask_existingId_deletesSuccessfully() {
        when(taskRepository.existsById(1L)).thenReturn(true);

        assertThatCode(() -> taskService.deleteTask(1L)).doesNotThrowAnyException();
        verify(taskRepository).deleteById(1L);
    }

    @Test
    void deleteTask_missingId_throwsNotFound() {
        when(taskRepository.existsById(99L)).thenReturn(false);

        assertThatThrownBy(() -> taskService.deleteTask(99L))
                .isInstanceOf(TaskNotFoundException.class);
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private Task buildTask(Long id, String title, Task.Status status) {
        return Task.builder()
                .id(id)
                .title(title)
                .status(status)
                .priority(Task.Priority.MEDIUM)
                .build();
    }
}
