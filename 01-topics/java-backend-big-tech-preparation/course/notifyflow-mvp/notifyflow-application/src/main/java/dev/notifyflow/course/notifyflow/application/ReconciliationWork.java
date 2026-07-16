package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.DeliveryAttempt;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.ReconciliationCase;
import dev.notifyflow.course.notifyflow.domain.AttemptStatus;
import dev.notifyflow.course.notifyflow.domain.ReconciliationStatus;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;

import java.util.Objects;

/** Consistent snapshots returned by an atomic OPEN-to-QUERYING claim. */
public record ReconciliationWork(
        ReconciliationCase reconciliationCase,
        NotificationTask task,
        DeliveryAttempt attempt) {

    public ReconciliationWork {
        Objects.requireNonNull(reconciliationCase, "reconciliationCase");
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(attempt, "attempt");
        if (reconciliationCase.taskId() != task.id()
                || !reconciliationCase.attemptId().equals(attempt.attemptId())
                || attempt.taskId() != task.id()) {
            throw new IllegalArgumentException("reconciliation, task and attempt must refer to one delivery");
        }
        if (reconciliationCase.status() != ReconciliationStatus.QUERYING
                || task.status() != TaskStatus.UNKNOWN
                || attempt.status() != AttemptStatus.UNKNOWN) {
            throw new IllegalArgumentException("claimed reconciliation work must be QUERYING/UNKNOWN");
        }
    }
}
