package dev.notifyflow.course.notifyflow.domain;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class StateTransitionTest {

    @Test
    void taskAllowsOnlyDocumentedTransitions() {
        assertTrue(TaskStatus.ACCEPTED.canTransitionTo(TaskStatus.SENDING));
        assertTrue(TaskStatus.SENDING.canTransitionTo(TaskStatus.UNKNOWN));
        assertTrue(TaskStatus.UNKNOWN.canTransitionTo(TaskStatus.MANUAL_REVIEW));
        assertFalse(TaskStatus.UNKNOWN.canTransitionTo(TaskStatus.SENDING));
        assertFalse(TaskStatus.SUCCEEDED.canTransitionTo(TaskStatus.SENDING));
        assertThrows(IllegalStateException.class,
                () -> TaskStatus.UNKNOWN.transitionTo(TaskStatus.SENDING));
    }

    @Test
    void attemptCannotBeBlindlyRetriedFromUnknown() {
        assertTrue(AttemptStatus.SENDING.canTransitionTo(AttemptStatus.UNKNOWN));
        assertTrue(AttemptStatus.UNKNOWN.canTransitionTo(AttemptStatus.SUCCEEDED));
        assertFalse(AttemptStatus.UNKNOWN.canTransitionTo(AttemptStatus.SENDING));
        assertThrows(IllegalStateException.class,
                () -> AttemptStatus.UNKNOWN.transitionTo(AttemptStatus.SENDING));
    }

    @Test
    void outboxAllowsRetryButNotResurrection() {
        assertTrue(OutboxStatus.PENDING.canTransitionTo(OutboxStatus.RETRY));
        assertTrue(OutboxStatus.RETRY.canTransitionTo(OutboxStatus.PUBLISHED));
        assertTrue(OutboxStatus.RETRY.canTransitionTo(OutboxStatus.RETRY));
        assertFalse(OutboxStatus.PUBLISHED.canTransitionTo(OutboxStatus.RETRY));
    }

    @Test
    void reconciliationCanReopenAfterQuery() {
        assertTrue(ReconciliationStatus.OPEN.canTransitionTo(ReconciliationStatus.QUERYING));
        assertTrue(ReconciliationStatus.QUERYING.canTransitionTo(ReconciliationStatus.OPEN));
        assertTrue(ReconciliationStatus.QUERYING.canTransitionTo(ReconciliationStatus.RESOLVED));
        assertFalse(ReconciliationStatus.RESOLVED.canTransitionTo(ReconciliationStatus.OPEN));
    }
}
