package dev.notifyflow.course.notifyflow.domain;

import org.junit.jupiter.api.Test;

import java.time.Instant;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ImmutableAggregateTest {
    private static final Instant T0 = Instant.parse("2026-07-15T00:00:00Z");
    private static final Instant T1 = T0.plusSeconds(1);

    @Test
    void taskTransitionReturnsNewValueAndIncrementsVersion() {
        NotificationTask accepted = new NotificationTask(1L, "tenant", "request", "fingerprint",
                "COURSE_STUB", "recipient", "WELCOME_V1", "{\"name\":\"fixture\"}",
                TaskStatus.ACCEPTED, 0, null, null, 0L, T0, T0);

        NotificationTask sending = accepted.startAttempt(T1);

        assertNotSame(accepted, sending);
        assertEquals(TaskStatus.ACCEPTED, accepted.status());
        assertEquals(TaskStatus.SENDING, sending.status());
        assertEquals(1, sending.currentAttemptNo());
        assertEquals(1L, sending.version());
        assertThrows(IllegalStateException.class, () -> sending.startAttempt(T1.plusSeconds(1)));
    }

    @Test
    void attemptTransitionSetsFinishTimeWithoutMutatingOriginal() {
        DeliveryAttempt sending = new DeliveryAttempt("attempt-1", 1L, 1, "COURSE_STUB",
                "task-1-attempt-1", null, AttemptStatus.SENDING, T1.plusSeconds(10),
                null, null, 0L, T0, null);

        DeliveryAttempt unknown = sending.transitionTo(AttemptStatus.UNKNOWN, T1, null, "TIMEOUT", "PROVIDER_TIMEOUT");

        assertEquals(AttemptStatus.SENDING, sending.status());
        assertEquals(AttemptStatus.UNKNOWN, unknown.status());
        assertEquals(T1, unknown.finishedAt());
        assertEquals(1L, unknown.version());
    }
}
