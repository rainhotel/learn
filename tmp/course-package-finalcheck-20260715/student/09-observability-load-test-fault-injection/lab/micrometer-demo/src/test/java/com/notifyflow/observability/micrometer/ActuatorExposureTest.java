package com.notifyflow.observability.micrometer;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest(
        classes = NotifyFlowApplication.class,
        webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
class ActuatorExposureTest {

    private final HttpClient httpClient = HttpClient.newBuilder()
            .connectTimeout(Duration.ofSeconds(5))
            .build();

    @Value("${local.server.port}")
    private int port;

    @Autowired
    private NotifyFlowMetrics metrics;

    @Test
    void healthAndMetricsAreExposedButEnvironmentIsNot() throws Exception {
        metrics.recordProviderRequest("mock", "success", Duration.ofMillis(20));

        HttpResponse<String> health = get("/actuator/health");
        HttpResponse<String> metricsIndex = get("/actuator/metrics");
        HttpResponse<String> customMetric = get(
                "/actuator/metrics/" + NotifyFlowMetrics.PROVIDER_REQUEST
        );
        HttpResponse<String> environment = get("/actuator/env");

        assertEquals(200, health.statusCode());
        assertEquals(200, metricsIndex.statusCode());
        assertEquals(200, customMetric.statusCode());
        assertTrue(customMetric.body().contains(NotifyFlowMetrics.PROVIDER_REQUEST));
        assertEquals(404, environment.statusCode());
    }

    private HttpResponse<String> get(String path) throws Exception {
        HttpRequest request = HttpRequest.newBuilder()
                .uri(URI.create("http://127.0.0.1:" + port + path))
                .timeout(Duration.ofSeconds(5))
                .GET()
                .build();
        return httpClient.send(request, HttpResponse.BodyHandlers.ofString());
    }
}
