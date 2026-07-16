package dev.notifyflow.course.notifyflow.application;

import java.time.Instant;
import java.util.Objects;

/** Store-ready normalized data. The store owns the transaction and generated task id. */
public record CreateTaskDraft(
        String tenantId,
        String requestId,
        String requestFingerprint,
        String channel,
        String recipientRef,
        String templateCode,
        String variablesJson,
        String eventId,
        Instant createdAt) {

    public CreateTaskDraft {
        tenantId = required(tenantId, "tenantId");
        requestId = required(requestId, "requestId");
        requestFingerprint = required(requestFingerprint, "requestFingerprint");
        channel = required(channel, "channel");
        recipientRef = required(recipientRef, "recipientRef");
        templateCode = required(templateCode, "templateCode");
        variablesJson = required(variablesJson, "variablesJson");
        eventId = required(eventId, "eventId");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
