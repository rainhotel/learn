package dev.notifyflow.course.notifyflow.boot.worker;

import dev.notifyflow.course.notifyflow.application.EventPublisher;
import dev.notifyflow.course.notifyflow.application.TaskAcceptedEvent;
import dev.notifyflow.course.notifyflow.domain.OutboxMessage;

import java.util.Optional;
import java.util.concurrent.ConcurrentLinkedQueue;

/**
 * Course-mode broker boundary used until the Kafka adapter is added. Publishing
 * and consuming remain separate scheduled steps, so the outbox transaction is
 * never extended across delivery work.
 */
public final class CourseTaskAcceptedEventBridge implements EventPublisher {
    private final ConcurrentLinkedQueue<TaskAcceptedEvent> queue = new ConcurrentLinkedQueue<>();

    @Override
    public void publish(OutboxMessage message) {
        if (!"TaskAccepted".equals(message.eventType()) || message.eventVersion() != 1) {
            throw new IllegalArgumentException("unsupported event contract: "
                    + message.eventType() + " v" + message.eventVersion());
        }
        long taskId;
        try {
            taskId = Long.parseLong(message.aggregateId());
        } catch (NumberFormatException exception) {
            throw new IllegalArgumentException("TaskAccepted aggregateId must be a long", exception);
        }
        queue.add(new TaskAcceptedEvent(message.eventId(), taskId));
    }

    public Optional<TaskAcceptedEvent> poll() {
        return Optional.ofNullable(queue.poll());
    }

    public void requeue(TaskAcceptedEvent event) {
        queue.add(event);
    }

    public int pending() {
        return queue.size();
    }
}
