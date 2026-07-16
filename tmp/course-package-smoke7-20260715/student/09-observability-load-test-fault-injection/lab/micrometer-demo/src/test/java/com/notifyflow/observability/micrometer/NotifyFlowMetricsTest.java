package com.notifyflow.observability.micrometer;

import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import java.time.Duration;
import java.util.List;
import java.util.concurrent.TimeUnit;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(classes = NotifyFlowApplication.class)
class NotifyFlowMetricsTest {

    @Autowired
    private MeterRegistry registry;

    @Autowired
    private NotifyFlowMetrics metrics;

    @Test
    void providerAndResultCreateOnlySixLowCardinalityTimers() {
        List<String> providers = List.of("aliyun", "tencent", "mock");
        List<String> results = List.of("success", "failure");

        for (String provider : providers) {
            for (String result : results) {
                metrics.recordProviderRequest(provider, result, Duration.ofMillis(125));
            }
        }

        assertEquals(6, registry.find(NotifyFlowMetrics.PROVIDER_REQUEST).timers().size());
        Timer aliyunSuccess = registry.find(NotifyFlowMetrics.PROVIDER_REQUEST)
                .tags("provider", "aliyun", "result", "success")
                .timer();
        assertEquals(1L, aliyunSuccess.count());
        assertEquals(125.0D, aliyunSuccess.totalTime(TimeUnit.MILLISECONDS), 0.001D);
    }

    @Test
    void dangerousIdentityTagsAreDeniedBeforeRegistration() {
        long metersBefore = registry.getMeters().size();
        Counter denied = Counter.builder("notifyflow.dangerous")
                .tag("taskId", "task-10001")
                .register(registry);
        denied.increment();

        assertNull(registry.find("notifyflow.dangerous").counter());
        assertEquals(metersBefore, registry.getMeters().size());
        assertTrue(MetricTagPolicy.isForbidden("taskId"));
        assertTrue(MetricTagPolicy.isForbidden("trace_id"));
        assertTrue(MetricTagPolicy.isForbidden("user-id"));
        assertFalse(MetricTagPolicy.isForbidden("provider"));
        assertFalse(MetricTagPolicy.isForbidden("result"));
    }
}
