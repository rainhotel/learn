package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.AttemptStatus;
import dev.notifyflow.course.notifyflow.domain.DeliveryAttempt;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.ReconciliationCase;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Applies only provider query facts; absence of a fact keeps the task UNKNOWN until its deadline. */
public final class ReconcileUnknownService {
    private final ReconciliationStore reconciliationStore;
    private final ProviderQueryGateway providerQueryGateway;
    private final Clock clock;
    private final Duration retryDelay;

    public ReconcileUnknownService(
            ReconciliationStore reconciliationStore,
            ProviderQueryGateway providerQueryGateway,
            Clock clock,
            Duration retryDelay) {
        this.reconciliationStore = Objects.requireNonNull(reconciliationStore, "reconciliationStore");
        this.providerQueryGateway = Objects.requireNonNull(providerQueryGateway, "providerQueryGateway");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.retryDelay = positive(retryDelay, "retryDelay");
    }

    public ReconcileUnknownResult reconcileOne() {
        Instant claimedAt = Objects.requireNonNull(clock.now(), "clock.now()");
        Optional<ReconciliationWork> claimed = reconciliationStore.claimDue(claimedAt);
        if (claimed.isEmpty()) {
            return new ReconcileUnknownResult(ReconcileUnknownResult.Outcome.NO_DUE_CASE, null);
        }

        ReconciliationWork work = claimed.orElseThrow();
        ProviderQueryResult providerResult = Objects.requireNonNull(providerQueryGateway.query(
                new ProviderQueryCommand(work.reconciliationCase().caseId(), work.attempt().attemptId(),
                        work.task().id(), work.attempt().providerCode(), work.attempt().idempotencyKey(),
                        work.attempt().providerRequestId(), work.reconciliationCase().deadlineAt())),
                "providerQueryGateway.query()");
        Instant completedAt = Objects.requireNonNull(clock.now(), "clock.now()");
        Decision decision = decide(work, providerResult, completedAt);
        if (!reconciliationStore.complete(work, decision.completion())) {
            return new ReconcileUnknownResult(ReconcileUnknownResult.Outcome.OPTIMISTIC_CONFLICT,
                    work.reconciliationCase().caseId());
        }
        return new ReconcileUnknownResult(decision.outcome(), work.reconciliationCase().caseId());
    }

    private Decision decide(ReconciliationWork work, ProviderQueryResult result, Instant at) {
        NotificationTask task = work.task();
        DeliveryAttempt attempt = work.attempt();
        ReconciliationCase reconciliationCase = work.reconciliationCase();
        return switch (result.status()) {
            case SUCCEEDED -> new Decision(ReconcileUnknownResult.Outcome.RESOLVED,
                    new ReconciliationCompletion(
                            reconciliationCase.resolve(result.status().name(), at),
                            task.transitionTo(TaskStatus.SUCCEEDED, at),
                            attempt.transitionTo(AttemptStatus.SUCCEEDED, at,
                                    prefer(result.providerRequestId(), attempt.providerRequestId()), null, null)));
            case REJECTED -> new Decision(ReconcileUnknownResult.Outcome.RESOLVED,
                    new ReconciliationCompletion(
                            reconciliationCase.resolve(result.status().name(), at),
                            task.transitionTo(TaskStatus.FAILED, at,
                                    result.errorCategory(), result.errorCode()),
                            attempt.transitionTo(AttemptStatus.PERMANENT_FAILED, at,
                                    prefer(result.providerRequestId(), attempt.providerRequestId()),
                                    result.errorCategory(), result.errorCode())));
            case PENDING, NOT_FOUND -> unresolved(work, result, at);
        };
    }

    private Decision unresolved(ReconciliationWork work, ProviderQueryResult result, Instant at) {
        if (!at.isBefore(work.reconciliationCase().deadlineAt())) {
            return new Decision(ReconcileUnknownResult.Outcome.MANUAL_REVIEW,
                    new ReconciliationCompletion(
                            work.reconciliationCase().moveToManualReview(result.status().name(), at),
                            work.task().transitionTo(TaskStatus.MANUAL_REVIEW, at,
                                    "RECONCILIATION", "DEADLINE_EXCEEDED"),
                            work.attempt()));
        }
        return new Decision(ReconcileUnknownResult.Outcome.RETRY_SCHEDULED,
                new ReconciliationCompletion(
                        work.reconciliationCase().reopen(at.plus(retryDelay), result.status().name(), at),
                        work.task(),
                        work.attempt()));
    }

    private static String prefer(String first, String fallback) {
        return first == null || first.isBlank() ? fallback : first;
    }

    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative() || value.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return value;
    }

    private record Decision(
            ReconcileUnknownResult.Outcome outcome,
            ReconciliationCompletion completion) {
    }
}
