package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.AttemptStatus;
import dev.notifyflow.course.notifyflow.domain.DeliveryAttempt;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

class ConsumeTaskAcceptedServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");

    @Test
    void successConvergesTaskAndAttemptWithoutReconciliation() {
        CapturingDeliveryStore store = new CapturingDeliveryStore(work());
        ConsumeTaskAcceptedResult result = service(store,
                command -> ProviderCallResult.success("provider-1")).consume(event());

        assertEquals(ConsumeTaskAcceptedResult.Outcome.PROCESSED, result.outcome());
        assertEquals(TaskStatus.SUCCEEDED, store.completion.task().status());
        assertEquals(AttemptStatus.SUCCEEDED, store.completion.attempt().status());
        assertNull(store.completion.reconciliationCase());
    }

    @Test
    void deterministicRejectionBecomesPermanentFailure() {
        CapturingDeliveryStore store = new CapturingDeliveryStore(work());
        service(store, command -> ProviderCallResult.rejected(
                "provider-2", "BUSINESS", "RECIPIENT_BLOCKED")).consume(event());

        assertEquals(TaskStatus.FAILED, store.completion.task().status());
        assertEquals(AttemptStatus.PERMANENT_FAILED, store.completion.attempt().status());
        assertEquals("RECIPIENT_BLOCKED", store.completion.task().lastErrorCode());
        assertNull(store.completion.reconciliationCase());
    }

    @Test
    void ambiguousCallCreatesExactlyOneOpenReconciliationRequest() {
        CapturingDeliveryStore store = new CapturingDeliveryStore(work());
        service(store, command -> ProviderCallResult.unknown(
                "provider-3", "UNKNOWN", "READ_TIMEOUT")).consume(event());

        assertEquals(TaskStatus.UNKNOWN, store.completion.task().status());
        assertEquals(AttemptStatus.UNKNOWN, store.completion.attempt().status());
        assertNotNull(store.completion.reconciliationCase());
        assertEquals("case-2", store.completion.reconciliationCase().caseId());
        assertEquals(NOW.plusSeconds(10), store.completion.reconciliationCase().nextQueryAt());
        assertEquals(NOW.plusSeconds(310), store.completion.reconciliationCase().deadlineAt());
    }

    @Test
    void duplicateEventDoesNotCallProvider() {
        CapturingDeliveryStore store = new CapturingDeliveryStore(null);
        int[] providerCalls = {0};
        ConsumeTaskAcceptedResult result = service(store, command -> {
            providerCalls[0]++;
            return ProviderCallResult.success("impossible");
        }).consume(event());

        assertEquals(ConsumeTaskAcceptedResult.Outcome.DUPLICATE_OR_BUSY, result.outcome());
        assertEquals(0, providerCalls[0]);
    }

    private static ConsumeTaskAcceptedService service(DeliveryStore store, ProviderGateway gateway) {
        return new ConsumeTaskAcceptedService(store, gateway, () -> NOW, new SequenceIds(), "COURSE_STUB",
                Duration.ofSeconds(2), Duration.ofSeconds(10), Duration.ofMinutes(5));
    }

    private static TaskAcceptedEvent event() {
        return new TaskAcceptedEvent("event-1", 1L);
    }

    private static DeliveryWork work() {
        NotificationTask task = new NotificationTask(1L, "tenant", "request", "a".repeat(64),
                "COURSE_STUB", "recipient", "WELCOME", "{}", TaskStatus.SENDING,
                1, null, null, 1L, NOW.minusSeconds(10), NOW);
        DeliveryAttempt attempt = new DeliveryAttempt("attempt-1", 1L, 1, "COURSE_STUB",
                "attempt-1", null, AttemptStatus.SENDING, NOW.plusSeconds(2), null, null,
                0L, NOW, null);
        return new DeliveryWork(task, attempt);
    }

    private static final class CapturingDeliveryStore implements DeliveryStore {
        private final DeliveryWork work;
        private DeliveryCompletion completion;

        private CapturingDeliveryStore(DeliveryWork work) {
            this.work = work;
        }

        @Override
        public Optional<DeliveryWork> tryStart(long taskId, String attemptId, String providerCode,
                                                String idempotencyKey, Instant startedAt, Instant deadlineAt) {
            return Optional.ofNullable(work);
        }

        @Override
        public boolean complete(DeliveryWork expected, DeliveryCompletion completion) {
            this.completion = completion;
            return true;
        }
    }

    private static final class SequenceIds implements IdGenerator {
        private int next;

        @Override
        public String nextId() {
            next++;
            return next == 1 ? "attempt-1" : "case-" + next;
        }
    }
}
