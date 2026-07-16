package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.DeliveryAttempt;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.AttemptStatus;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;

import java.util.Objects;

/** Task and attempt snapshots returned by the atomic ACCEPTED-to-SENDING claim. */
public record DeliveryWork(NotificationTask task, DeliveryAttempt attempt) {
    public DeliveryWork {
        Objects.requireNonNull(task, "task");
        Objects.requireNonNull(attempt, "attempt");
        if (attempt.taskId() != task.id()) {
            throw new IllegalArgumentException("attempt must belong to task");
        }
        if (task.status() != TaskStatus.SENDING || attempt.status() != AttemptStatus.SENDING) {
            throw new IllegalArgumentException("claimed delivery work must be SENDING");
        }
    }
}
