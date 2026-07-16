package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.OutboxMessage;
import dev.notifyflow.course.notifyflow.domain.OutboxStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.Objects;

/** Publishes a leased batch without holding a database transaction across broker I/O. */
public final class PublishOutboxService {
    private final OutboxStore outboxStore;
    private final EventPublisher eventPublisher;
    private final Clock clock;
    private final String leaseOwner;
    private final Duration leaseDuration;
    private final Duration retryDelay;
    private final int maxAttempts;
    private final int batchSize;

    public PublishOutboxService(
            OutboxStore outboxStore,
            EventPublisher eventPublisher,
            Clock clock,
            String leaseOwner,
            Duration leaseDuration,
            Duration retryDelay,
            int maxAttempts,
            int batchSize) {
        this.outboxStore = Objects.requireNonNull(outboxStore, "outboxStore");
        this.eventPublisher = Objects.requireNonNull(eventPublisher, "eventPublisher");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.leaseOwner = required(leaseOwner, "leaseOwner");
        this.leaseDuration = positive(leaseDuration, "leaseDuration");
        this.retryDelay = positive(retryDelay, "retryDelay");
        if (maxAttempts <= 0 || batchSize <= 0) {
            throw new IllegalArgumentException("maxAttempts and batchSize must be positive");
        }
        this.maxAttempts = maxAttempts;
        this.batchSize = batchSize;
    }

    public PublishBatchResult publishBatch() {
        Instant now = Objects.requireNonNull(clock.now(), "clock.now()");
        List<OutboxMessage> claimed = List.copyOf(outboxStore.claimDue(
                now, leaseOwner, now.plus(leaseDuration), batchSize));
        int published = 0;
        int retries = 0;
        int failed = 0;
        int conflicts = 0;

        for (OutboxMessage message : claimed) {
            try {
                eventPublisher.publish(message);
            } catch (RuntimeException publishFailure) {
                Instant failedAt = Objects.requireNonNull(clock.now(), "clock.now()");
                String error = errorSummary(publishFailure);
                OutboxMessage updated;
                boolean terminal = message.attemptCount() + 1 >= maxAttempts;
                if (terminal) {
                    updated = message.markFailed(error, failedAt);
                } else {
                    updated = scheduleRetry(message, failedAt.plus(retryDelay), error, failedAt);
                }
                if (!outboxStore.save(message, updated)) {
                    conflicts++;
                } else if (terminal) {
                    failed++;
                } else {
                    retries++;
                }
                continue;
            }

            // A broker acknowledgement followed by a database failure must not be
            // rewritten as a broker failure. Let the exception escape: after the
            // lease expires, at-least-once publication safely repeats the event.
            OutboxMessage updated = message.markPublished(Objects.requireNonNull(clock.now(), "clock.now()"));
            if (outboxStore.save(message, updated)) {
                published++;
            } else {
                conflicts++;
            }
        }
        return new PublishBatchResult(claimed.size(), published, retries, failed, conflicts);
    }

    private static OutboxMessage scheduleRetry(
            OutboxMessage message, Instant nextAttemptAt, String error, Instant at) {
        if (message.status() == OutboxStatus.PENDING) {
            return message.scheduleRetry(nextAttemptAt, error, at);
        }
        if (message.status() != OutboxStatus.RETRY) {
            throw new IllegalStateException("Only PENDING/RETRY outbox messages may retry: " + message.status());
        }
        return new OutboxMessage(message.id(), message.eventId(), message.aggregateType(),
                message.aggregateId(), message.eventType(), message.eventVersion(), message.partitionKey(),
                message.payload(), OutboxStatus.RETRY, message.attemptCount() + 1, nextAttemptAt,
                null, null, null, error, message.version() + 1, message.createdAt(), at);
    }

    private static String errorSummary(RuntimeException failure) {
        String message = failure.getMessage();
        return failure.getClass().getSimpleName() + (message == null || message.isBlank() ? "" : ": " + message);
    }

    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isZero() || value.isNegative()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
