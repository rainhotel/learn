package dev.notifyflow.course.notifyflow.application;

import java.time.Instant;
import java.util.Optional;

/** Transaction boundary for unique delivery claim and optimistic completion. */
public interface DeliveryStore {
    Optional<DeliveryWork> tryStart(
            long taskId,
            String attemptId,
            String providerCode,
            String idempotencyKey,
            Instant startedAt,
            Instant deadlineAt);

    boolean complete(DeliveryWork expected, DeliveryCompletion completion);
}
