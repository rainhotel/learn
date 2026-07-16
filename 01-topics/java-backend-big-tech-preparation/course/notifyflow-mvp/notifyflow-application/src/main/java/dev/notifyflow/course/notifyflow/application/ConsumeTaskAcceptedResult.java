package dev.notifyflow.course.notifyflow.application;

import java.util.Objects;

/** Consumer decision; only PROCESSED safely acknowledges the new provider result. */
public record ConsumeTaskAcceptedResult(Outcome outcome, String attemptId) {
    public enum Outcome {
        PROCESSED,
        DUPLICATE_OR_BUSY,
        OPTIMISTIC_CONFLICT
    }

    public ConsumeTaskAcceptedResult {
        outcome = Objects.requireNonNull(outcome, "outcome");
        if (outcome == Outcome.DUPLICATE_OR_BUSY) {
            if (attemptId != null) {
                throw new IllegalArgumentException("duplicate/busy outcome must not expose an unclaimed attempt");
            }
        } else if (attemptId == null || attemptId.isBlank()) {
            throw new IllegalArgumentException("processed/conflict outcome requires attemptId");
        }
    }
}
