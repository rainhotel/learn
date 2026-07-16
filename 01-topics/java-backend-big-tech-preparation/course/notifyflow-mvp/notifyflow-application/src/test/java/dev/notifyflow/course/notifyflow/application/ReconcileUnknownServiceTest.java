package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.AttemptStatus;
import dev.notifyflow.course.notifyflow.domain.DeliveryAttempt;
import dev.notifyflow.course.notifyflow.domain.NotificationTask;
import dev.notifyflow.course.notifyflow.domain.ReconciliationCase;
import dev.notifyflow.course.notifyflow.domain.ReconciliationStatus;
import dev.notifyflow.course.notifyflow.domain.TaskStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

class ReconcileUnknownServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");

    @Test
    void explicitSuccessResolvesUnknownDelivery() {
        CapturingStore store = new CapturingStore(work(NOW.plusSeconds(60)));
        ReconcileUnknownResult result = service(store,
                command -> ProviderQueryResult.succeeded("provider-1")).reconcileOne();

        assertEquals(ReconcileUnknownResult.Outcome.RESOLVED, result.outcome());
        assertEquals(TaskStatus.SUCCEEDED, store.completion.task().status());
        assertEquals(AttemptStatus.SUCCEEDED, store.completion.attempt().status());
        assertEquals(ReconciliationStatus.RESOLVED, store.completion.reconciliationCase().status());
    }

    @Test
    void pendingBeforeDeadlineKeepsBusinessStateUnknownAndSchedulesNextQuery() {
        CapturingStore store = new CapturingStore(work(NOW.plusSeconds(60)));
        ReconcileUnknownResult result = service(store,
                command -> ProviderQueryResult.pending("provider-1")).reconcileOne();

        assertEquals(ReconcileUnknownResult.Outcome.RETRY_SCHEDULED, result.outcome());
        assertEquals(TaskStatus.UNKNOWN, store.completion.task().status());
        assertEquals(AttemptStatus.UNKNOWN, store.completion.attempt().status());
        assertEquals(ReconciliationStatus.OPEN, store.completion.reconciliationCase().status());
        assertEquals(NOW.plusSeconds(15), store.completion.reconciliationCase().nextQueryAt());
    }

    @Test
    void notFoundAtDeadlineMovesToManualReviewInsteadOfInventingFailure() {
        CapturingStore store = new CapturingStore(work(NOW));
        ReconcileUnknownResult result = service(store,
                command -> ProviderQueryResult.notFound()).reconcileOne();

        assertEquals(ReconcileUnknownResult.Outcome.MANUAL_REVIEW, result.outcome());
        assertEquals(TaskStatus.MANUAL_REVIEW, store.completion.task().status());
        assertEquals(AttemptStatus.UNKNOWN, store.completion.attempt().status());
        assertEquals(ReconciliationStatus.MANUAL_REVIEW,
                store.completion.reconciliationCase().status());
    }

    @Test
    void reportsNoDueCaseWithoutCallingProvider() {
        CapturingStore store = new CapturingStore(null);
        int[] calls = {0};
        ReconcileUnknownResult result = service(store, command -> {
            calls[0]++;
            return ProviderQueryResult.notFound();
        }).reconcileOne();

        assertEquals(ReconcileUnknownResult.Outcome.NO_DUE_CASE, result.outcome());
        assertEquals(0, calls[0]);
    }

    private static ReconcileUnknownService service(
            ReconciliationStore store, ProviderQueryGateway gateway) {
        return new ReconcileUnknownService(store, gateway, () -> NOW, Duration.ofSeconds(15));
    }

    private static ReconciliationWork work(Instant deadline) {
        NotificationTask task = new NotificationTask(1L, "tenant", "request", "a".repeat(64),
                "COURSE_STUB", "recipient", "WELCOME", "{}", TaskStatus.UNKNOWN,
                1, "UNKNOWN", "READ_TIMEOUT", 2L, NOW.minusSeconds(20), NOW.minusSeconds(10));
        DeliveryAttempt attempt = new DeliveryAttempt("attempt-1", 1L, 1, "COURSE_STUB",
                "attempt-1", "provider-1", AttemptStatus.UNKNOWN, NOW.minusSeconds(15),
                "UNKNOWN", "READ_TIMEOUT", 1L, NOW.minusSeconds(20), NOW.minusSeconds(10));
        ReconciliationCase reconciliationCase = new ReconciliationCase("case-1", 1L, "attempt-1",
                ReconciliationStatus.QUERYING, 1, NOW, deadline, "PENDING", 1L,
                NOW.minusSeconds(10), NOW, null);
        return new ReconciliationWork(reconciliationCase, task, attempt);
    }

    private static final class CapturingStore implements ReconciliationStore {
        private final ReconciliationWork work;
        private ReconciliationCompletion completion;

        private CapturingStore(ReconciliationWork work) {
            this.work = work;
        }

        @Override
        public Optional<ReconciliationWork> claimDue(Instant now) {
            return Optional.ofNullable(work);
        }

        @Override
        public boolean complete(ReconciliationWork expected, ReconciliationCompletion completion) {
            this.completion = completion;
            return true;
        }
    }
}
