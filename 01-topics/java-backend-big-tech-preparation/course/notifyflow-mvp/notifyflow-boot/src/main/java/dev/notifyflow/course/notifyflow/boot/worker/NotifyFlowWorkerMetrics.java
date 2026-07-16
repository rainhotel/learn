package dev.notifyflow.course.notifyflow.boot.worker;

import dev.notifyflow.course.notifyflow.application.ConsumeTaskAcceptedResult;
import dev.notifyflow.course.notifyflow.application.PublishBatchResult;
import dev.notifyflow.course.notifyflow.application.ReconcileUnknownResult;

import io.micrometer.core.instrument.MeterRegistry;

import java.util.Locale;
import java.util.Objects;

/** Fixed-label worker metrics. No business identifier is ever used as a tag. */
public final class NotifyFlowWorkerMetrics {
    private static final String EVENT_TYPE = "TaskAccepted";
    private static final String PROVIDER = "course-stub";
    private final MeterRegistry registry;

    public NotifyFlowWorkerMetrics(MeterRegistry registry) {
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    public void record(PublishBatchResult result) {
        incrementOutbox("published", result.published());
        incrementOutbox("retry", result.retryScheduled());
        incrementOutbox("failed", result.failed());
        incrementOutbox("optimistic_conflict", result.optimisticConflicts());
    }

    public void record(ConsumeTaskAcceptedResult result) {
        String label = switch (result.outcome()) {
            case PROCESSED -> "claimed";
            case DUPLICATE_OR_BUSY -> "duplicate";
            case OPTIMISTIC_CONFLICT -> "optimistic_conflict";
        };
        registry.counter("notifyflow.kafka.consume", "result", label, "eventType", EVENT_TYPE).increment();
    }

    public void record(ReconcileUnknownResult result) {
        if (result.outcome() == ReconcileUnknownResult.Outcome.NO_DUE_CASE) {
            return;
        }
        String label = result.outcome().name().toLowerCase(Locale.ROOT);
        registry.counter("notifyflow.reconciliation", "provider", PROVIDER, "result", label).increment();
    }

    public void recordWorkerFailure(String worker) {
        String label = switch (worker) {
            case "publisher", "consumer", "reconciler" -> worker;
            default -> "unknown";
        };
        registry.counter("notifyflow.worker.failure", "worker", label).increment();
    }

    private void incrementOutbox(String result, int amount) {
        if (amount > 0) {
            registry.counter("notifyflow.outbox.publish", "result", result, "eventType", EVENT_TYPE)
                    .increment(amount);
        }
    }
}
