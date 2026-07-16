package dev.notifyflow.course.notifyflow.domain;

import java.time.Instant;
import java.util.Objects;

/** Immutable outbox row; Kafka I/O happens outside the database transaction. */
public record OutboxMessage(
        long id,
        String eventId,
        String aggregateType,
        String aggregateId,
        String eventType,
        int eventVersion,
        String partitionKey,
        String payload,
        OutboxStatus status,
        int attemptCount,
        Instant nextAttemptAt,
        String leaseOwner,
        Instant leaseUntil,
        Instant publishedAt,
        String lastError,
        long version,
        Instant createdAt,
        Instant updatedAt) {

    public OutboxMessage {
        if (id <= 0) {
            throw new IllegalArgumentException("id must be positive");
        }
        eventId = required(eventId, "eventId");
        aggregateType = required(aggregateType, "aggregateType");
        aggregateId = required(aggregateId, "aggregateId");
        eventType = required(eventType, "eventType");
        if (eventVersion <= 0) {
            throw new IllegalArgumentException("eventVersion must be positive");
        }
        partitionKey = required(partitionKey, "partitionKey");
        payload = required(payload, "payload");
        status = Objects.requireNonNull(status, "status");
        if (attemptCount < 0 || version < 0) {
            throw new IllegalArgumentException("attemptCount/version must not be negative");
        }
        nextAttemptAt = Objects.requireNonNull(nextAttemptAt, "nextAttemptAt");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public OutboxMessage markPublished(Instant at) {
        Objects.requireNonNull(at, "at");
        status.transitionTo(OutboxStatus.PUBLISHED);
        return copy(OutboxStatus.PUBLISHED, attemptCount, nextAttemptAt, null, null, at, null, version + 1, at);
    }

    public OutboxMessage scheduleRetry(Instant next, String error, Instant at) {
        Objects.requireNonNull(next, "next");
        Objects.requireNonNull(at, "at");
        status.transitionTo(OutboxStatus.RETRY);
        return copy(OutboxStatus.RETRY, attemptCount + 1, next, null, null, null, error, version + 1, at);
    }

    public OutboxMessage markFailed(String error, Instant at) {
        Objects.requireNonNull(at, "at");
        status.transitionTo(OutboxStatus.FAILED);
        return copy(OutboxStatus.FAILED, attemptCount + 1, nextAttemptAt, null, null, null, error, version + 1, at);
    }

    private OutboxMessage copy(OutboxStatus nextStatus, int nextAttemptCount, Instant nextAttempt,
                               String nextLeaseOwner, Instant nextLeaseUntil, Instant nextPublished,
                               String nextError, long nextVersion, Instant at) {
        return new OutboxMessage(id, eventId, aggregateType, aggregateId, eventType, eventVersion,
                partitionKey, payload, nextStatus, nextAttemptCount, nextAttempt, nextLeaseOwner,
                nextLeaseUntil, nextPublished, nextError, nextVersion, createdAt, at);
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
