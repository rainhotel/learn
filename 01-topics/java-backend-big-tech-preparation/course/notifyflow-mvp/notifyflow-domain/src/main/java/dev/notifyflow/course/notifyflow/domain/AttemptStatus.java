package dev.notifyflow.course.notifyflow.domain;

/** Lifecycle of one provider delivery attempt. */
public enum AttemptStatus {
    SENDING,
    UNKNOWN,
    SUCCEEDED,
    PERMANENT_FAILED;

    public boolean canTransitionTo(AttemptStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case SENDING -> target == SUCCEEDED || target == PERMANENT_FAILED || target == UNKNOWN;
            case UNKNOWN -> target == SUCCEEDED || target == PERMANENT_FAILED;
            case SUCCEEDED, PERMANENT_FAILED -> false;
        };
    }

    public AttemptStatus transitionTo(AttemptStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException("Illegal attempt transition: " + this + " -> " + target);
        }
        return target;
    }

    public boolean isTerminal() {
        return this == SUCCEEDED || this == PERMANENT_FAILED;
    }
}
