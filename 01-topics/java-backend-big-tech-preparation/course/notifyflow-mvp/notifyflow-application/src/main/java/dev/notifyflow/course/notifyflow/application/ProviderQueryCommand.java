package dev.notifyflow.course.notifyflow.application;

import java.time.Instant;
import java.util.Objects;

/** Provider fact lookup for an attempt whose delivery result is UNKNOWN. */
public record ProviderQueryCommand(
        String caseId,
        String attemptId,
        long taskId,
        String providerCode,
        String idempotencyKey,
        String providerRequestId,
        Instant deadlineAt) {

    public ProviderQueryCommand {
        caseId = required(caseId, "caseId");
        attemptId = required(attemptId, "attemptId");
        if (taskId <= 0) {
            throw new IllegalArgumentException("taskId must be positive");
        }
        providerCode = required(providerCode, "providerCode");
        idempotencyKey = required(idempotencyKey, "idempotencyKey");
        deadlineAt = Objects.requireNonNull(deadlineAt, "deadlineAt");
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
