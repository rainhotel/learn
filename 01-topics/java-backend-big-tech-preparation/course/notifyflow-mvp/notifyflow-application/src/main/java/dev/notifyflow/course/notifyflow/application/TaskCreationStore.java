package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.NotificationTask;

import java.util.Objects;

/** Persistence port for the atomic task plus outbox create transaction. */
public interface TaskCreationStore {

    CreationOutcome create(CreateTaskDraft draft);

    enum OutcomeStatus {
        CREATED,
        REPLAYED,
        CONFLICT
    }

    record CreationOutcome(OutcomeStatus status, NotificationTask task) {
        public CreationOutcome {
            status = Objects.requireNonNull(status, "status");
            if (task == null) {
                throw new IllegalArgumentException("task must be present for a creation outcome");
            }
        }
    }
}
