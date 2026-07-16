package dev.notifyflow.course.notifyflow.domain;

/** Delivery state of an outbox row. */
public enum OutboxStatus {
    PENDING,
    RETRY,
    PUBLISHED,
    FAILED;

    public boolean canTransitionTo(OutboxStatus target) {
        if (target == null || target == this) {
            return false;
        }
        return switch (this) {
            case PENDING -> target == RETRY || target == PUBLISHED || target == FAILED;
            case RETRY -> target == RETRY || target == PUBLISHED || target == FAILED;
            case PUBLISHED, FAILED -> false;
        };
    }

    public OutboxStatus transitionTo(OutboxStatus target) {
        if (!canTransitionTo(target)) {
            throw new IllegalStateException("Illegal outbox transition: " + this + " -> " + target);
        }
        return target;
    }

    public boolean isTerminal() {
        return this == PUBLISHED || this == FAILED;
    }
}
