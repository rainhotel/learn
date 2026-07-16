package dev.notifyflow.course.notifyflow.boot.worker;

import dev.notifyflow.course.notifyflow.application.ConsumeTaskAcceptedResult;
import dev.notifyflow.course.notifyflow.application.ConsumeTaskAcceptedService;
import dev.notifyflow.course.notifyflow.application.PublishBatchResult;
import dev.notifyflow.course.notifyflow.application.PublishOutboxService;
import dev.notifyflow.course.notifyflow.application.ReconcileUnknownResult;
import dev.notifyflow.course.notifyflow.application.ReconcileUnknownService;
import dev.notifyflow.course.notifyflow.application.TaskAcceptedEvent;
import dev.notifyflow.course.notifyflow.boot.config.NotifyFlowRuntimeProperties;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

/** Bounded publisher, course-consumer/delivery, and reconciliation loops. */
@Component
@ConditionalOnProperty(prefix = "notifyflow.workers", name = "enabled", matchIfMissing = true)
public final class NotifyFlowScheduledWorkers {
    private final PublishOutboxService publisher;
    private final CourseTaskAcceptedEventBridge bridge;
    private final ConsumeTaskAcceptedService consumer;
    private final ReconcileUnknownService reconciler;
    private final NotifyFlowWorkerMetrics metrics;
    private final int consumerBatchSize;

    public NotifyFlowScheduledWorkers(
            PublishOutboxService publisher,
            CourseTaskAcceptedEventBridge bridge,
            ConsumeTaskAcceptedService consumer,
            ReconcileUnknownService reconciler,
            NotifyFlowWorkerMetrics metrics,
            NotifyFlowRuntimeProperties properties) {
        this.publisher = publisher;
        this.bridge = bridge;
        this.consumer = consumer;
        this.reconciler = reconciler;
        this.metrics = metrics;
        this.consumerBatchSize = properties.consumerBatchSize();
    }

    @Scheduled(
            fixedDelayString = "${notifyflow.workers.publisher-fixed-delay:200ms}",
            initialDelayString = "${notifyflow.workers.initial-delay:1s}")
    public void publishOutbox() {
        try {
            PublishBatchResult result = publisher.publishBatch();
            metrics.record(result);
        } catch (RuntimeException failure) {
            metrics.recordWorkerFailure("publisher");
        }
    }

    @Scheduled(
            fixedDelayString = "${notifyflow.workers.consumer-fixed-delay:100ms}",
            initialDelayString = "${notifyflow.workers.initial-delay:1s}")
    public void consumeAndDeliver() {
        for (int processed = 0; processed < consumerBatchSize; processed++) {
            TaskAcceptedEvent event = bridge.poll().orElse(null);
            if (event == null) {
                return;
            }
            try {
                ConsumeTaskAcceptedResult result = consumer.consume(event);
                metrics.record(result);
                if (result.outcome() == ConsumeTaskAcceptedResult.Outcome.OPTIMISTIC_CONFLICT) {
                    bridge.requeue(event);
                    return;
                }
            } catch (RuntimeException failure) {
                bridge.requeue(event);
                metrics.recordWorkerFailure("consumer");
                return;
            }
        }
    }

    @Scheduled(
            fixedDelayString = "${notifyflow.workers.reconciler-fixed-delay:500ms}",
            initialDelayString = "${notifyflow.workers.initial-delay:1s}")
    public void reconcileUnknown() {
        try {
            ReconcileUnknownResult result = reconciler.reconcileOne();
            metrics.record(result);
        } catch (RuntimeException failure) {
            metrics.recordWorkerFailure("reconciler");
        }
    }
}
