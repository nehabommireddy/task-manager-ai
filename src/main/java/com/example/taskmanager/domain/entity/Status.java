package com.example.taskmanager.domain.entity;

/**
 * Lifecycle state of a task. Ordered from least to most complete —
 * useful if ordering by status ever becomes a requirement.
 */
public enum Status {
    TODO,
    IN_PROGRESS,
    DONE
}
