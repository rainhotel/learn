package dev.notifyflow.course.notifyflow.domain;

/** Lifecycle of a notification task. */
public enum TaskStatus {
    ACCEPTED,
    SENDING,
    UNKNOWN,
    SUCCEEDED,
    FAILED,
    MANUAL_REVIEW;

    public boolean canTransitionTo(TaskStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case ACCEPTED -> target == SENDING;
            case SENDING -> target == SUCCEEDED || target == FAILED || target == UNKNOWN;
            case UNKNOWN -> target == SUCCEEDED || target == FAILED || target == MANUAL_REVIEW;
            case SUCCEEDED, FAILED, MANUAL_REVIEW -> false;
        };
    }

    public TaskStatus transitionTo(TaskStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException("Illegal task transition: " + this + " -> " + target);
        }
        return target;
    }

    public boolean isTerminal() {
        return this == SUCCEEDED || this == FAILED || this == MANUAL_REVIEW;
    }
}
