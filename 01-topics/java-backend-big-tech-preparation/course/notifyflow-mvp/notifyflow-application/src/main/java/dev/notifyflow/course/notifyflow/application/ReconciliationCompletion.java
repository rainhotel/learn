package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.DeliveryAttempt;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.ReconciliationCase;
import dev.notifyflow.course.notifyflow.domain.ReconciliationStatus;

import java.util.Objects;

/** Atomic state requested after one provider fact query. */
public record ReconciliationCompletion(
        ReconciliationCase reconciliationCase,
        NotificationTask task,
        DeliveryAttempt attempt) {

    public ReconciliationCompletion {
        Objects.requireNonNull(reconciliationCase, "reconciliationCase");
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(attempt, "attempt");
        if (reconciliationCase.taskId() != task.id()
                || !reconciliationCase.attemptId().equals(attempt.attemptId())
                || attempt.taskId() != task.id()) {
            throw new IllegalArgumentException("completion values must refer to one delivery");
        }
        if (reconciliationCase.status() == ReconciliationStatus.QUERYING) {
            throw new IllegalArgumentException("completion must leave QUERYING");
        }
    }
}
