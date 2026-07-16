package dev.notifyflow.course.notifyflow.application;

import java.time.Instant;
import java.util.Optional;

/** Transaction boundary for due-case claiming and optimistic fact application. */
public interface ReconciliationStore {
    Optional<ReconciliationWork> claimDue(Instant now);

    boolean complete(ReconciliationWork expected, ReconciliationCompletion completion);
}
