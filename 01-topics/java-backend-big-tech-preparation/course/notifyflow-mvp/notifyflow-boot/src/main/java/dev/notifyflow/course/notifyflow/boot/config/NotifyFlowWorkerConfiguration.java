package dev.notifyflow.course.notifyflow.boot.config;

import dev.notifyflow.course.notifyflow.application.Clock;
import dev.notifyflow.course.notifyflow.application.ConsumeTaskAcceptedService;
import dev.notifyflow.course.notifyflow.application.DeliveryStore;
import dev.notifyflow.course.notifyflow.application.EventPublisher;
import dev.notifyflow.course.notifyflow.application.IdGenerator;
import dev.notifyflow.course.notifyflow.application.OutboxStore;
import dev.notifyflow.course.notifyflow.application.ProviderGateway;
import dev.notifyflow.course.notifyflow.application.ProviderQueryGateway;
import dev.notifyflow.course.notifyflow.application.PublishOutboxService;
import dev.notifyflow.course.notifyflow.application.ReconcileUnknownService;
import dev.notifyflow.course.notifyflow.application.ReconciliationStore;
import dev.notifyflow.course.notifyflow.boot.worker.CourseTaskAcceptedEventBridge;
import dev.notifyflow.course.notifyflow.boot.worker.NotifyFlowWorkerMetrics;
import dev.notifyflow.course.notifyflow.infrastructure.provider.JdkHttpProviderClient;

import io.micrometer.core.instrument.MeterRegistry;

import java.net.URI;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.annotation.EnableScheduling;

/** Composition root for Phase 2-3 workers; core state rules remain in application. */
@Configuration
@EnableScheduling
@EnableConfigurationProperties(NotifyFlowRuntimeProperties.class)
public class NotifyFlowWorkerConfiguration {

    @Bean
    CourseTaskAcceptedEventBridge courseTaskAcceptedEventBridge() {
        return new CourseTaskAcceptedEventBridge();
    }

    @Bean
    MeteredProviderClient meteredProviderClient(
            NotifyFlowRuntimeProperties properties,
            MeterRegistry meterRegistry,
            @Value("${notifyflow.provider.base-url:http://localhost:8081}") URI providerBaseUrl) {
        JdkHttpProviderClient client = new JdkHttpProviderClient(
                providerBaseUrl,
                properties.providerConnectTimeout(),
                properties.providerRequestTimeout());
        return new MeteredProviderClient(client, client, meterRegistry);
    }

    @Bean
    PublishOutboxService publishOutboxService(
            OutboxStore outboxStore,
            EventPublisher eventPublisher,
            Clock clock,
            NotifyFlowRuntimeProperties properties) {
        return new PublishOutboxService(
                outboxStore,
                eventPublisher,
                clock,
                properties.leaseOwner(),
                properties.outboxLeaseDuration(),
                properties.outboxRetryDelay(),
                properties.outboxMaxAttempts(),
                properties.outboxBatchSize());
    }

    @Bean
    ConsumeTaskAcceptedService consumeTaskAcceptedService(
            DeliveryStore deliveryStore,
            ProviderGateway providerGateway,
            Clock clock,
            IdGenerator idGenerator,
            NotifyFlowRuntimeProperties properties) {
        return new ConsumeTaskAcceptedService(
                deliveryStore,
                providerGateway,
                clock,
                idGenerator,
                properties.providerCode(),
                properties.providerDeadline(),
                properties.reconciliationDelay(),
                properties.reconciliationWindow());
    }

    @Bean
    ReconcileUnknownService reconcileUnknownService(
            ReconciliationStore reconciliationStore,
            ProviderQueryGateway providerQueryGateway,
            Clock clock,
            NotifyFlowRuntimeProperties properties) {
        return new ReconcileUnknownService(
                reconciliationStore,
                providerQueryGateway,
                clock,
                properties.reconciliationRetryDelay());
    }

    @Bean
    NotifyFlowWorkerMetrics notifyFlowWorkerMetrics(MeterRegistry meterRegistry) {
        return new NotifyFlowWorkerMetrics(meterRegistry);
    }
}
