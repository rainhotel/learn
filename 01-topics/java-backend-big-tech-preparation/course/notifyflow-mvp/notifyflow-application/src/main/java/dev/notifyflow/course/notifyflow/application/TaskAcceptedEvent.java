package dev.notifyflow.course.notifyflow.application;

import java.util.Objects;

/** Version-one broker payload reduced to the identifiers needed for conditional claiming. */
public record TaskAcceptedEvent(String eventId, long taskId) {
    public TaskAcceptedEvent {
        eventId = required(eventId, "eventId");
        if (taskId <= 0) {
            throw new IllegalArgumentException("taskId must be positive");
        }
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
