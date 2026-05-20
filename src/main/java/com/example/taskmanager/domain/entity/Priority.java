package com.example.taskmanager.domain.entity;

/**
 * Importance level of a task. MEDIUM is the safe default —
 * callers only need to specify priority when they mean to deviate.
 */
public enum Priority {
    LOW,
    MEDIUM,
    HIGH
}
