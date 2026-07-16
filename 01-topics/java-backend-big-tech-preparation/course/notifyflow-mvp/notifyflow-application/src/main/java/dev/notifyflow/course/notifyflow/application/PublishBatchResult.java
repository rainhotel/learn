package dev.notifyflow.course.notifyflow.application;

/** Low-cardinality summary of one bounded outbox drain. */
public record PublishBatchResult(
        int claimed,
        int published,
        int retryScheduled,
        int failed,
        int optimisticConflicts) {

    public PublishBatchResult {
        if (claimed < 0 || published < 0 || retryScheduled < 0 || failed < 0 || optimisticConflicts < 0) {
            throw new IllegalArgumentException("publish counts must not be negative");
        }
        if (published + retryScheduled + failed + optimisticConflicts != claimed) {
            throw new IllegalArgumentException("every claimed message must have exactly one outcome");
        }
    }
}
