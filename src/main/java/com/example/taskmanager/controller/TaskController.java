package com.example.taskmanager.controller;

import com.example.taskmanager.domain.dto.TaskDtos.CreateTaskRequest;
import com.example.taskmanager.domain.dto.TaskDtos.TaskResponse;
import com.example.taskmanager.domain.dto.TaskDtos.UpdateTaskRequest;
import com.example.taskmanager.domain.entity.Priority;
import com.example.taskmanager.domain.entity.Status;
import com.example.taskmanager.service.TaskService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

@CrossOrigin(origins = "*")
@RestController
@RequestMapping("/api/tasks")
@RequiredArgsConstructor
public class TaskController {

    private final TaskService taskService;

    @GetMapping
    public ResponseEntity<List<TaskResponse>> getAllTasks(
            @RequestParam(required = false) Status status,
            @RequestParam(required = false) Priority priority
    ) {
        return ResponseEntity.ok(taskService.getAllTasks(status, priority));
    }

    @GetMapping("/{id}")
    public ResponseEntity<TaskResponse> getTaskById(@PathVariable Long id) {
        return ResponseEntity.ok(taskService.getTaskById(id));
    }

    @PostMapping
    public ResponseEntity<TaskResponse> createTask(@Valid @RequestBody CreateTaskRequest request) {
        TaskResponse created = taskService.createTask(request);

        // Location header points the client directly to the new resource —
        // standard REST practice, lets clients avoid a follow-up GET.
        URI location = ServletUriComponentsBuilder.fromCurrentRequest()
                .path("/{id}")
                .buildAndExpand(created.id())
                .toUri();

        return ResponseEntity.created(location).body(created);
    }

    @PutMapping("/{id}")
    public ResponseEntity<TaskResponse> replaceTask(
            @PathVariable Long id,
            @Valid @RequestBody CreateTaskRequest request
    ) {
        // PUT replaces the entire resource — all fields are required/defaulted.
        // We reuse CreateTaskRequest because it carries the same complete field set.
        // PATCH (/api/tasks/{id}) remains available for partial updates.
        return ResponseEntity.ok(taskService.replaceTask(id, request));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<TaskResponse> updateTask(
            @PathVariable Long id,
            @Valid @RequestBody UpdateTaskRequest request
    ) {
        return ResponseEntity.ok(taskService.updateTask(id, request));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable Long id) {
        taskService.deleteTask(id);
        return ResponseEntity.noContent().build();
    }
}
