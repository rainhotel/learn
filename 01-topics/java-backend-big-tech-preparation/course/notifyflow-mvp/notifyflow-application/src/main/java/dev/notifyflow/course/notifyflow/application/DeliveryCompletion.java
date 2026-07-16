package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.AttemptStatus;
import dev.notifyflow.course.notifyflow.domain.DeliveryAttempt;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.ReconciliationCase;
import dev.notifyflow.course.notifyflow.domain.ReconciliationStatus;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;

import java.util.Objects;

/** Atomic completion requested after provider I/O has finished outside the transaction. */
public record DeliveryCompletion(
        NotificationTask task,
        DeliveryAttempt attempt,
        ReconciliationCase reconciliationCase) {

    public DeliveryCompletion {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(attempt, "attempt");
        if (attempt.taskId() != task.id()) {
            throw new IllegalArgumentException("attempt must belong to task");
        }
        boolean success = task.status() == TaskStatus.SUCCEEDED && attempt.status() == AttemptStatus.SUCCEEDED;
        boolean rejected = task.status() == TaskStatus.FAILED
                && attempt.status() == AttemptStatus.PERMANENT_FAILED;
        boolean unknown = task.status() == TaskStatus.UNKNOWN && attempt.status() == AttemptStatus.UNKNOWN
                && reconciliationCase != null
                && reconciliationCase.status() == ReconciliationStatus.OPEN
                && reconciliationCase.taskId() == task.id()
                && reconciliationCase.attemptId().equals(attempt.attemptId());
        if (!success && !rejected && !unknown) {
            throw new IllegalArgumentException("task, attempt and reconciliation states are inconsistent");
        }
        if (!unknown && reconciliationCase != null) {
            throw new IllegalArgumentException("only UNKNOWN completion may create reconciliation");
        }
    }
}
