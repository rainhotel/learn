package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.OutboxMessage;
import dev.notifyflow.course.notifyflow.domain.OutboxStatus;
import org.junit.jupiter.api.Test;

import java.time.Duration;
import java.time.Instant;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class PublishOutboxServiceTest {
    private static final Instant NOW = Instant.parse("2026-07-15T10:00:00Z");

    @Test
    void marksBrokerAcknowledgedMessagePublished() {
        CapturingStore store = new CapturingStore(message(OutboxStatus.PENDING, 0));
        PublishOutboxService service = service(store, ignored -> { }, 3);

        PublishBatchResult result = service.publishBatch();

        assertEquals(new PublishBatchResult(1, 1, 0, 0, 0), result);
        assertEquals(OutboxStatus.PUBLISHED, store.updated.status());
        assertEquals(NOW, store.updated.publishedAt());
    }

    @Test
    void schedulesRetryWithoutClaimingBrokerFailureAsSuccess() {
        CapturingStore store = new CapturingStore(message(OutboxStatus.PENDING, 0));
        PublishOutboxService service = service(store, ignored -> {
            throw new IllegalStateException("broker unavailable");
        }, 3);

        PublishBatchResult result = service.publishBatch();

        assertEquals(new PublishBatchResult(1, 0, 1, 0, 0), result);
        assertEquals(OutboxStatus.RETRY, store.updated.status());
        assertEquals(1, store.updated.attemptCount());
        assertEquals(NOW.plusSeconds(5), store.updated.nextAttemptAt());
    }

    @Test
    void exhaustsRetryBudgetAndReportsOptimisticConflictSeparately() {
        CapturingStore terminalStore = new CapturingStore(message(OutboxStatus.RETRY, 1));
        PublishBatchResult terminal = service(terminalStore, ignored -> {
            throw new IllegalStateException("still unavailable");
        }, 2).publishBatch();
        assertEquals(new PublishBatchResult(1, 0, 0, 1, 0), terminal);
        assertEquals(OutboxStatus.FAILED, terminalStore.updated.status());

        CapturingStore conflictStore = new CapturingStore(message(OutboxStatus.PENDING, 0));
        conflictStore.saveResult = false;
        PublishBatchResult conflict = service(conflictStore, ignored -> { }, 2).publishBatch();
        assertEquals(new PublishBatchResult(1, 0, 0, 0, 1), conflict);
    }

    @Test
    void doesNotMisclassifyDatabaseFailureAfterBrokerAcknowledgement() {
        CapturingStore store = new CapturingStore(message(OutboxStatus.PENDING, 0));
        store.saveFailure = new IllegalStateException("database unavailable");

        assertThrows(IllegalStateException.class,
                () -> service(store, ignored -> { }, 3).publishBatch());
        assertEquals(OutboxStatus.PUBLISHED, store.updated.status());
    }

    private static PublishOutboxService service(
            OutboxStore store, EventPublisher publisher, int maxAttempts) {
        return new PublishOutboxService(store, publisher, () -> NOW, "worker-1",
                Duration.ofSeconds(30), Duration.ofSeconds(5), maxAttempts, 10);
    }

    private static OutboxMessage message(OutboxStatus status, int attempts) {
        return new OutboxMessage(1L, "event-1", "NotificationTask", "1", "TaskAcceptedV1",
                1, "1", "{}", status, attempts, NOW.minusSeconds(1), "worker-1",
                NOW.plusSeconds(30), null, null, attempts, NOW.minusSeconds(10), NOW);
    }

    private static final class CapturingStore implements OutboxStore {
        private final OutboxMessage claimed;
        private OutboxMessage updated;
        private boolean saveResult = true;
        private RuntimeException saveFailure;

        private CapturingStore(OutboxMessage claimed) {
            this.claimed = claimed;
        }

        @Override
        public List<OutboxMessage> claimDue(Instant now, String leaseOwner, Instant leaseUntil, int limit) {
            return List.of(claimed);
        }

        @Override
        public boolean save(OutboxMessage expected, OutboxMessage updated) {
            this.updated = updated;
            if (saveFailure != null) {
                throw saveFailure;
            }
            return saveResult;
        }
    }
}
