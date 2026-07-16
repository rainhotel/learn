package com.notifyflow.observability.micrometer;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.Set;

@Component
public final class NotifyFlowMetrics {

    public static final String PROVIDER_REQUEST = "notifyflow.provider.request";

    private static final Set<String> ALLOWED_PROVIDERS = Set.of(
            "aliyun",
            "tencent",
            "mock"
    );
    private static final Set<String> ALLOWED_RESULTS = Set.of(
            "success",
            "failure"
    );

    private final MeterRegistry registry;

    public NotifyFlowMetrics(MeterRegistry registry) {
        this.registry = registry;
    }

    public void recordProviderRequest(
            String provider,
            String result,
            Duration duration
    ) {
        requireAllowed(provider, ALLOWED_PROVIDERS, "provider");
        requireAllowed(result, ALLOWED_RESULTS, "result");
        if (duration == null || duration.isNegative()) {
            throw new IllegalArgumentException("duration must not be null or negative");
        }

        Timer.builder(PROVIDER_REQUEST)
                .description("NotifyFlow provider request latency")
                .tag("provider", provider)
                .tag("result", result)
                .publishPercentileHistogram()
                .register(registry)
                .record(duration);
    }

    private static void requireAllowed(
            String value,
            Set<String> allowlist,
            String label
    ) {
        if (!allowlist.contains(value)) {
            throw new IllegalArgumentException(label + " is outside the metric tag allowlist");
        }
    }
}
