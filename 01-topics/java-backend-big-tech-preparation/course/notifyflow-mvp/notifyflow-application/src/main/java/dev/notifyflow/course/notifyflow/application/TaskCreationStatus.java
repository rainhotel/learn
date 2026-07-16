package dev.notifyflow.course.notifyflow.application;

/** Outcome of an idempotent create request. */
public enum TaskCreationStatus {
    CREATED,
    REPLAYED,
    CONFLICT
}
