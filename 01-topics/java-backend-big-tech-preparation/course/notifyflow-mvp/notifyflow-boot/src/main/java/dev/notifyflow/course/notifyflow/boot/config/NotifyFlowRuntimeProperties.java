package dev.notifyflow.course.notifyflow.boot.config;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

/** Bounded course-runtime settings; every duration and batch has a safe default. */
@ConfigurationProperties("notifyflow.runtime")
public record NotifyFlowRuntimeProperties(
        Duration providerConnectTimeout,
        Duration providerRequestTimeout,
        Duration providerDeadline,
        Duration reconciliationDelay,
        Duration reconciliationWindow,
        Duration reconciliationRetryDelay,
        Duration outboxLeaseDuration,
        Duration outboxRetryDelay,
        int outboxMaxAttempts,
        int outboxBatchSize,
        int consumerBatchSize,
        String leaseOwner,
        String providerCode) {

    public NotifyFlowRuntimeProperties {
        providerConnectTimeout = positive(providerConnectTimeout, Duration.ofSeconds(1), "providerConnectTimeout");
        providerRequestTimeout = positive(providerRequestTimeout, Duration.ofMillis(750), "providerRequestTimeout");
        providerDeadline = positive(providerDeadline, Duration.ofSeconds(1), "providerDeadline");
        reconciliationDelay = positive(reconciliationDelay, Duration.ofSeconds(1), "reconciliationDelay");
        reconciliationWindow = positive(reconciliationWindow, Duration.ofMinutes(5), "reconciliationWindow");
        reconciliationRetryDelay = positive(reconciliationRetryDelay, Duration.ofSeconds(2), "reconciliationRetryDelay");
        outboxLeaseDuration = positive(outboxLeaseDuration, Duration.ofSeconds(10), "outboxLeaseDuration");
        outboxRetryDelay = positive(outboxRetryDelay, Duration.ofSeconds(1), "outboxRetryDelay");
        outboxMaxAttempts = positive(outboxMaxAttempts, 8, "outboxMaxAttempts");
        outboxBatchSize = positive(outboxBatchSize, 32, "outboxBatchSize");
        consumerBatchSize = positive(consumerBatchSize, 32, "consumerBatchSize");
        leaseOwner = required(leaseOwner, "notifyflow-course-node", "leaseOwner");
        providerCode = required(providerCode, "course-stub", "providerCode");
    }

    private static Duration positive(Duration value, Duration fallback, String name) {
        Duration result = value == null ? fallback : value;
        if (result.isNegative() || result.isZero()) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return result;
    }

    private static int positive(int value, int fallback, String name) {
        int result = value == 0 ? fallback : value;
        if (result < 0) {
            throw new IllegalArgumentException(name + " must be positive");
        }
        return result;
    }

    private static String required(String value, String fallback, String name) {
        String result = value == null || value.isBlank() ? fallback : value.trim();
        if (result.isEmpty()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return result;
    }
}
