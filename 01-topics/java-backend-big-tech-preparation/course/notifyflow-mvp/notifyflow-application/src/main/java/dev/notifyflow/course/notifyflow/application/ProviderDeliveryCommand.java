package dev.notifyflow.course.notifyflow.application;

import java.time.Instant;
import java.util.Objects;

/** Provider request with an explicit deadline and stable idempotency key. */
public record ProviderDeliveryCommand(
        String attemptId,
        long taskId,
        String tenantId,
        String channel,
        String recipientRef,
        String templateCode,
        String variablesJson,
        String idempotencyKey,
        Instant deadlineAt) {

    public ProviderDeliveryCommand {
        attemptId = required(attemptId, "attemptId");
        if (taskId <= 0) {
            throw new IllegalArgumentException("taskId must be positive");
        }
        tenantId = required(tenantId, "tenantId");
        channel = required(channel, "channel");
        recipientRef = required(recipientRef, "recipientRef");
        templateCode = required(templateCode, "templateCode");
        variablesJson = required(variablesJson, "variablesJson");
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
