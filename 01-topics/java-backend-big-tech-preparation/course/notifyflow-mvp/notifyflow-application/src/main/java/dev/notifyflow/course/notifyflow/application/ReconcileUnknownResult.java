package dev.notifyflow.course.notifyflow.application;

import java.util.Objects;

/** Low-cardinality result of processing at most one due reconciliation case. */
public record ReconcileUnknownResult(Outcome outcome, String caseId) {
    public enum Outcome {
        NO_DUE_CASE,
        RESOLVED,
        RETRY_SCHEDULED,
        MANUAL_REVIEW,
        OPTIMISTIC_CONFLICT
    }

    public ReconcileUnknownResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
        if (outcome == Outcome.NO_DUE_CASE) {
            if (caseId != null) {
                throw new IllegalArgumentException("no-due-case outcome must not expose caseId");
            }
        } else if (caseId == null || caseId.isBlank()) {
            throw new IllegalArgumentException("processed reconciliation outcome requires caseId");
        }
    }
}
