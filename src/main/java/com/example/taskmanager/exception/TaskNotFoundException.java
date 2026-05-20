package com.example.taskmanager.exception;

public class TaskNotFoundException extends RuntimeException {

    // Primary constructor: used throughout the service layer for ID-based lookups
    public TaskNotFoundException(Long id) {
        super("Task not found with id: " + id);
    }

    // Secondary constructor: useful if a future lookup fails on something other than ID
    // (e.g. "Task not found for user X") without needing a new exception type
    public TaskNotFoundException(String message) {
        super(message);
    }
}
