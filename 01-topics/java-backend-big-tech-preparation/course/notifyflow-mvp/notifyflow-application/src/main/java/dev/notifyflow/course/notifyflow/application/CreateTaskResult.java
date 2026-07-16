package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.NotificationTask;

import java.util.Objects;

/** Output boundary for create-task; HTTP maps CONFLICT to 409. */
public record CreateTaskResult(
        TaskCreationStatus status,
        NotificationTask task,
        String requestFingerprint) {

    public CreateTaskResult {
        status = Objects.requireNonNull(status, "status");
        Objects.requireNonNull(task, "task");
        requestFingerprint = Objects.requireNonNull(requestFingerprint, "requestFingerprint");
    }

    public boolean replayed() {
        return status == TaskCreationStatus.REPLAYED;
    }

    public boolean conflict() {
        return status == TaskCreationStatus.CONFLICT;
    }
}
