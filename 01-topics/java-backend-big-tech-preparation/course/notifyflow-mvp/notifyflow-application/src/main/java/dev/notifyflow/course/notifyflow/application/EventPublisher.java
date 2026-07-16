package dev.notifyflow.course.notifyflow.application;

import dev.notifyflow.course.notifyflow.domain.OutboxMessage;

/** Broker boundary. Returning means the broker acknowledged the event. */
@FunctionalInterface
public interface EventPublisher {
    void publish(OutboxMessage message);
}
