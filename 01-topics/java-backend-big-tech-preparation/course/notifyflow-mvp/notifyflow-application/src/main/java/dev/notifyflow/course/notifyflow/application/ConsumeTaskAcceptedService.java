package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.AttemptStatus;
import dev.notifyflow.course.notifyflow.domain.DeliveryAttempt;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.ReconciliationCase;
import dev.notifyflow.course.notifyflow.domain.ReconciliationStatus;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;

import java.time.Duration;
import java.time.Instant;
import java.util.Objects;
import java.util.Optional;

/** Claims one accepted task, performs provider I/O, then atomically records the classified fact. */
public final class ConsumeTaskAcceptedService {
    private final DeliveryStore deliveryStore;
    private final ProviderGateway providerGateway;
    private final Clock clock;
    private final IdGenerator idGenerator;
    private final String providerCode;
    private final Duration providerDeadline;
    private final Duration reconciliationDelay;
    private final Duration reconciliationWindow;

    public ConsumeTaskAcceptedService(
            DeliveryStore deliveryStore,
            ProviderGateway providerGateway,
            Clock clock,
            IdGenerator idGenerator,
            String providerCode,
            Duration providerDeadline,
            Duration reconciliationDelay,
            Duration reconciliationWindow) {
        this.deliveryStore = Objects.requireNonNull(deliveryStore, "deliveryStore");
        this.providerGateway = Objects.requireNonNull(providerGateway, "providerGateway");
        this.clock = Objects.requireNonNull(clock, "clock");
        this.idGenerator = Objects.requireNonNull(idGenerator, "idGenerator");
        this.providerCode = required(providerCode, "providerCode");
        this.providerDeadline = positive(providerDeadline, "providerDeadline");
        this.reconciliationDelay = positive(reconciliationDelay, "reconciliationDelay");
        this.reconciliationWindow = positive(reconciliationWindow, "reconciliationWindow");
    }

    public ConsumeTaskAcceptedResult consume(TaskAcceptedEvent event) {
        Objects.requireNonNull(event, "event");
        Instant startedAt = Objects.requireNonNull(clock.now(), "clock.now()");
        String attemptId = generatedId();
        Instant deadlineAt = startedAt.plus(providerDeadline);
        Optional<DeliveryWork> claimed = deliveryStore.tryStart(
                event.taskId(), attemptId, providerCode, attemptId, startedAt, deadlineAt);
        if (claimed.isEmpty()) {
            return new ConsumeTaskAcceptedResult(
                    ConsumeTaskAcceptedResult.Outcome.DUPLICATE_OR_BUSY, null);
        }

        DeliveryWork work = claimed.orElseThrow();
        NotificationTask task = work.task();
        DeliveryAttempt attempt = work.attempt();
        ProviderCallResult providerResult = Objects.requireNonNull(providerGateway.deliver(
                new ProviderDeliveryCommand(attempt.attemptId(), task.id(), task.tenantId(), task.channel(),
                        task.recipientRef(), task.templateCode(), task.variablesJson(), attempt.idempotencyKey(),
                        attempt.deadlineAt())), "providerGateway.deliver()");
        Instant completedAt = Objects.requireNonNull(clock.now(), "clock.now()");
        DeliveryCompletion completion = completion(work, providerResult, completedAt);
        boolean saved = deliveryStore.complete(work, completion);
        return new ConsumeTaskAcceptedResult(saved
                ? ConsumeTaskAcceptedResult.Outcome.PROCESSED
                : ConsumeTaskAcceptedResult.Outcome.OPTIMISTIC_CONFLICT, attempt.attemptId());
    }

    private DeliveryCompletion completion(
            DeliveryWork work, ProviderCallResult result, Instant completedAt) {
        return switch (result.classification()) {
            case SUCCESS -> new DeliveryCompletion(
                    work.task().transitionTo(TaskStatus.SUCCEEDED, completedAt),
                    work.attempt().transitionTo(AttemptStatus.SUCCEEDED, completedAt,
                            result.providerRequestId(), null, null),
                    null);
            case REJECTED -> new DeliveryCompletion(
                    work.task().transitionTo(TaskStatus.FAILED, completedAt,
                            result.errorCategory(), result.errorCode()),
                    work.attempt().transitionTo(AttemptStatus.PERMANENT_FAILED, completedAt,
                            result.providerRequestId(), result.errorCategory(), result.errorCode()),
                    null);
            case UNKNOWN -> {
                NotificationTask unknownTask = work.task().transitionTo(TaskStatus.UNKNOWN, completedAt,
                        result.errorCategory(), result.errorCode());
                DeliveryAttempt unknownAttempt = work.attempt().transitionTo(AttemptStatus.UNKNOWN, completedAt,
                        result.providerRequestId(), result.errorCategory(), result.errorCode());
                ReconciliationCase reconciliationCase = new ReconciliationCase(
                        generatedId(), work.task().id(), work.attempt().attemptId(), ReconciliationStatus.OPEN,
                        0, completedAt.plus(reconciliationDelay), completedAt.plus(reconciliationWindow),
                        null, 0L, completedAt, completedAt, null);
                yield new DeliveryCompletion(unknownTask, unknownAttempt, reconciliationCase);
            }
        };
    }

    private String generatedId() {
        return required(idGenerator.nextId(), "generated id");
    }

    private static Duration positive(Duration value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isNegative() || value.isZero()) {
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
