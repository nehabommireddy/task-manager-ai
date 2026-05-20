package com.example.taskmanager.service;

import java.util.List;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.example.taskmanager.domain.dto.TaskDtos.CreateTaskRequest;
import com.example.taskmanager.domain.dto.TaskDtos.TaskResponse;
import com.example.taskmanager.domain.dto.TaskDtos.UpdateTaskRequest;
import com.example.taskmanager.domain.entity.Priority;
import com.example.taskmanager.domain.entity.Status;
import com.example.taskmanager.domain.entity.Task;
import com.example.taskmanager.exception.TaskNotFoundException;
import com.example.taskmanager.repository.TaskRepository;

import lombok.RequiredArgsConstructor;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TaskService {

    private final TaskRepository taskRepository;

    public List<TaskResponse> getAllTasks(Status status, Priority priority) {
        List<Task> tasks;

        if (status != null && priority != null) {
            tasks = taskRepository.findByStatusAndPriority(status, priority);
        } else if (status != null) {
            tasks = taskRepository.findByStatus(status);
        } else if (priority != null) {
            tasks = taskRepository.findByPriority(priority);
        } else {
            tasks = taskRepository.findAll();
        }

        return tasks.stream()
                .map(TaskResponse::from)
                .toList();
    }

    public TaskResponse getTaskById(Long id) {
        return taskRepository.findById(id)
                .map(TaskResponse::from)
                .orElseThrow(() -> new TaskNotFoundException(id));
    }

    @Transactional
    public TaskResponse createTask(CreateTaskRequest request) {
        // Defaults (TODO, MEDIUM) are declared via @Builder.Default on the entity —
        // we don't repeat them here. Null fields simply don't override the builder defaults.
        // IMPORTANT:
        // Builder defaults only apply when the setter is NOT called.
        // Passing null into .status(null) or .priority(null)
        // overrides the @Builder.Default values and causes a DB
        // NOT NULL constraint violation at runtime.
        //
        // This was the root cause of the "An unexpected error occurred"
        // failures across the app.
        Task.TaskBuilder builder = Task.builder()
                .title(request.title())
                .description(request.description())
                .dueDate(request.dueDate());

        if (request.status() != null) {
            builder.status(request.status());
        }

        if (request.priority() != null) {
            builder.priority(request.priority());
        }

        Task task = builder.build();

        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse replaceTask(Long id, CreateTaskRequest request) {
        // PUT semantics: the full resource is replaced. All fields are overwritten,
        // not merged. If a field is absent from the request, its value resets to the
        // entity default (status → TODO, priority → MEDIUM, description/dueDate → null).
        // This is the key distinction from PATCH, which leaves absent fields unchanged.
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        task.setTitle(request.title());
        task.setDescription(request.description());
        task.setStatus(request.status() != null ? request.status() : Status.TODO);
        task.setPriority(request.priority() != null ? request.priority() : Priority.MEDIUM);
        task.setDueDate(request.dueDate());

        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public TaskResponse updateTask(Long id, UpdateTaskRequest request) {
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));

        // Mutation logic lives on the entity — see Task.applyUpdate for the
        // null-sentinel limitation and the rationale for this delegation pattern.
        task.applyUpdate(request.title(), request.description(),
                request.status(), request.priority(), request.dueDate());

        return TaskResponse.from(taskRepository.saveAndFlush(task));
    }

    @Transactional
    public void deleteTask(Long id) {
        // findById rather than existsById + deleteById: one round-trip instead of two,
        // and we get a managed entity reference that deleteById can use directly.
        Task task = taskRepository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        taskRepository.delete(task);
    }
}
