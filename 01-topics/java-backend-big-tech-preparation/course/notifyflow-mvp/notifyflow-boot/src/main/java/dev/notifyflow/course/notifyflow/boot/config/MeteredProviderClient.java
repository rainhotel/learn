package dev.notifyflow.course.notifyflow.boot.config;

import dev.notifyflow.course.notifyflow.application.ProviderCallResult;
import dev.notifyflow.course.notifyflow.application.ProviderDeliveryCommand;
import dev.notifyflow.course.notifyflow.application.ProviderGateway;
import dev.notifyflow.course.notifyflow.application.ProviderQueryCommand;
import dev.notifyflow.course.notifyflow.application.ProviderQueryGateway;
import dev.notifyflow.course.notifyflow.application.ProviderQueryResult;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;

import java.util.Objects;

/** Metrics decorator with provider/result values drawn only from fixed enums. */
public final class MeteredProviderClient implements ProviderGateway, ProviderQueryGateway {
    private static final String PROVIDER = "course-stub";

    private final ProviderGateway deliveryDelegate;
    private final ProviderQueryGateway queryDelegate;
    private final MeterRegistry registry;

    public MeteredProviderClient(
            ProviderGateway deliveryDelegate,
            ProviderQueryGateway queryDelegate,
            MeterRegistry registry) {
        this.deliveryDelegate = Objects.requireNonNull(deliveryDelegate, "deliveryDelegate");
        this.queryDelegate = Objects.requireNonNull(queryDelegate, "queryDelegate");
        this.registry = Objects.requireNonNull(registry, "registry");
    }

    @Override
    public ProviderCallResult deliver(ProviderDeliveryCommand command) {
        Timer.Sample sample = Timer.start(registry);
        ProviderCallResult result = deliveryDelegate.deliver(command);
        sample.stop(Timer.builder("notifyflow.provider.request")
                .description("Provider delivery latency")
                .tag("provider", PROVIDER)
                .tag("result", deliveryResult(result))
                .register(registry));
        return result;
    }

    @Override
    public ProviderQueryResult query(ProviderQueryCommand command) {
        Timer.Sample sample = Timer.start(registry);
        ProviderQueryResult result = queryDelegate.query(command);
        String outcome = result.status().name().toLowerCase(java.util.Locale.ROOT);
        sample.stop(Timer.builder("notifyflow.provider.query")
                .description("Provider fact-query latency")
                .tag("provider", PROVIDER)
                .tag("result", outcome)
                .register(registry));
        return result;
    }

    private static String deliveryResult(ProviderCallResult result) {
        return switch (result.classification()) {
            case SUCCESS -> "success";
            case REJECTED -> "reject";
            case UNKNOWN -> switch (result.errorCategory()) {
                case "TIMEOUT" -> "timeout";
                case "IO" -> "io_error";
                default -> "unknown";
            };
        };
    }
}
