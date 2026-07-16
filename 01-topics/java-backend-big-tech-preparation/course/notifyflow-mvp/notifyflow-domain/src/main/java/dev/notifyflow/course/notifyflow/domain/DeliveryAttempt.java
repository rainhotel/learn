package dev.notifyflow.course.notifyflow.domain;

import java.time.Instant;
import java.util.Objects;

/** Immutable provider attempt. It never silently reuses an old attempt number. */
public record DeliveryAttempt(
        String attemptId,
        long taskId,
        int attemptNo,
        String providerCode,
        String idempotencyKey,
        String providerRequestId,
        AttemptStatus status,
        Instant deadlineAt,
        String errorCategory,
        String errorCode,
        long version,
        Instant startedAt,
        Instant finishedAt) {

    public DeliveryAttempt {
        attemptId = required(attemptId, "attemptId");
        if (taskId <= 0) {
            throw new IllegalArgumentException("taskId must be positive");
        }
        if (attemptNo <= 0) {
            throw new IllegalArgumentException("attemptNo must be positive");
        }
        providerCode = required(providerCode, "providerCode");
        idempotencyKey = required(idempotencyKey, "idempotencyKey");
        status = Objects.requireNonNull(status, "status");
        deadlineAt = Objects.requireNonNull(deadlineAt, "deadlineAt");
        startedAt = Objects.requireNonNull(startedAt, "startedAt");
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
    }

    public DeliveryAttempt transitionTo(AttemptStatus target, Instant at) {
        return transitionTo(target, at, providerRequestId, null, null);
    }

    public DeliveryAttempt transitionTo(
            AttemptStatus target,
            Instant at,
            String nextProviderRequestId,
            String nextErrorCategory,
            String nextErrorCode) {
        Objects.requireNonNull(at, "at");
        status.transitionTo(target);
        Instant nextFinishedAt = finishedAt == null ? at : finishedAt;
        return new DeliveryAttempt(attemptId, taskId, attemptNo, providerCode, idempotencyKey,
                nextProviderRequestId, target, deadlineAt, nextErrorCategory, nextErrorCode,
                version + 1, startedAt, nextFinishedAt);
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
