package dev.notifyflow.course.notifyflow.domain;

/** State of a provider fact lookup for an UNKNOWN attempt. */
public enum ReconciliationStatus {
    OPEN,
    QUERYING,
    RESOLVED,
    MANUAL_REVIEW;

    public boolean canTransitionTo(ReconciliationStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case OPEN -> target == QUERYING || target == RESOLVED || target == MANUAL_REVIEW;
            case QUERYING -> target == OPEN || target == RESOLVED || target == MANUAL_REVIEW;
            case RESOLVED, MANUAL_REVIEW -> false;
        };
    }

    public ReconciliationStatus transitionTo(ReconciliationStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException("Illegal reconciliation transition: " + this + " -> " + target);
        }
        return target;
    }

    public boolean isTerminal() {
        return this == RESOLVED || this == MANUAL_REVIEW;
    }
}
