package dev.notifyflow.course.notifyflow.domain;

import java.time.Instant;
import java.util.Objects;

/** Immutable persisted task aggregate. State changes return a new value. */
public record NotificationTask(
        long id,
        String tenantId,
        String requestId,
        String requestFingerprint,
        String channel,
        String recipientRef,
        String templateCode,
        String variablesJson,
        TaskStatus status,
        int currentAttemptNo,
        String lastErrorCategory,
        String lastErrorCode,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public NotificationTask {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        tenantId = required(tenantId, "tenantId");
        requestId = required(requestId, "requestId");
        requestFingerprint = required(requestFingerprint, "requestFingerprint");
        channel = required(channel, "channel");
        recipientRef = required(recipientRef, "recipientRef");
        templateCode = required(templateCode, "templateCode");
        variablesJson = required(variablesJson, "variablesJson");
        status = Objects.requireNonNull(status, "status");
        if (currentAttemptNo < 0) {
            throw new IllegalArgumentException("currentAttemptNo must not be negative");
        }
        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public NotificationTask startAttempt(Instant at) {
        Objects.requireNonNull(at, "at");
        if (status != TaskStatus.ACCEPTED) {
            throw new IllegalStateException("Only ACCEPTED tasks can start an attempt: " + status);
        }
        return copy(TaskStatus.SENDING, currentAttemptNo + 1, null, null, version + 1, at);
    }

    public NotificationTask transitionTo(TaskStatus target, Instant at) {
        return transitionTo(target, at, null, null);
    }

    public NotificationTask transitionTo(
            TaskStatus target,
            Instant at,
            String errorCategory,
            String errorCode) {
        Objects.requireNonNull(at, "at");
        status.transitionTo(target);
        return copy(target, currentAttemptNo, errorCategory, errorCode, version + 1, at);
    }

    private NotificationTask copy(
            TaskStatus nextStatus,
            int nextAttemptNo,
            String nextErrorCategory,
            String nextErrorCode,
            long nextVersion,
            Instant at) {
        return new NotificationTask(id, tenantId, requestId, requestFingerprint, channel,
                recipientRef, templateCode, variablesJson, nextStatus, nextAttemptNo,
                nextErrorCategory, nextErrorCode, nextVersion, createdAt, at);
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
