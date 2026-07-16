package dev.notifyflow.course.providerstub;

import java.nio.charset.StandardCharsets;
import java.time.Duration;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ConcurrentHashMap;

import org.springframework.stereotype.Service;

@Service
public class ProviderStubService {

    private final ProviderStubProperties properties;
    private final Map<String, ScenarioConfiguration> scenarios = new ConcurrentHashMap<>();
    private final Map<String, DeliveryFact> deliveryFacts = new ConcurrentHashMap<>();

    public ProviderStubService(ProviderStubProperties properties) {
        this.properties = properties;
    }

    public DeliveryOutcome deliver(String idempotencyKey, DeliveryRequest request) {
        requireKey(idempotencyKey);
        ScenarioConfiguration configured = scenarios.getOrDefault(
                idempotencyKey,
                new ScenarioConfiguration(ProviderScenario.SUCCEED, Duration.ZERO));

        DeliveryFact fact = deliveryFacts.computeIfAbsent(
                idempotencyKey,
                ignored -> createFact(idempotencyKey, configured, request));

        if (fact.scenario() == ProviderScenario.COMMIT_THEN_DELAY) {
            delay(fact.delay());
        }
        return fact.outcome();
    }

    public DeliveryOutcome query(String idempotencyKey) {
        requireKey(idempotencyKey);
        DeliveryFact fact = deliveryFacts.get(idempotencyKey);
        if (fact == null) {
            return new DeliveryOutcome(null, ProviderDeliveryStatus.NOT_FOUND, null);
        }
        return fact.outcome();
    }

    public ScenarioView configureScenario(
            String idempotencyKey,
            ProviderScenario scenario,
            Duration requestedDelay) {
        requireKey(idempotencyKey);
        if (scenario == null) {
            throw new IllegalArgumentException("scenario is required");
        }
        if (deliveryFacts.containsKey(idempotencyKey)) {
            throw new ScenarioAlreadyAppliedException(idempotencyKey);
        }

        Duration delay = resolveDelay(scenario, requestedDelay);
        ScenarioConfiguration configuration = new ScenarioConfiguration(scenario, delay);
        scenarios.put(idempotencyKey, configuration);
        return new ScenarioView(idempotencyKey, scenario, delay.toMillis());
    }

    public EffectsView effects(String idempotencyKey) {
        requireKey(idempotencyKey);
        DeliveryFact fact = deliveryFacts.get(idempotencyKey);
        if (fact == null) {
            return new EffectsView(idempotencyKey, 0, null, ProviderDeliveryStatus.NOT_FOUND);
        }
        return new EffectsView(
                idempotencyKey,
                fact.sideEffectCount(),
                fact.outcome().providerRequestId(),
                fact.outcome().status());
    }

    public void clearScenarios() {
        scenarios.clear();
    }

    void clearAllForTest() {
        scenarios.clear();
        deliveryFacts.clear();
    }

    private DeliveryFact createFact(
            String idempotencyKey,
            ScenarioConfiguration scenario,
            DeliveryRequest request) {
        String providerRequestId = stableProviderRequestId(idempotencyKey);
        return switch (scenario.scenario()) {
            case SUCCEED -> new DeliveryFact(
                    scenario.scenario(),
                    Duration.ZERO,
                    1,
                    new DeliveryOutcome(providerRequestId, ProviderDeliveryStatus.SUCCEEDED, null));
            case REJECT -> new DeliveryFact(
                    scenario.scenario(),
                    Duration.ZERO,
                    0,
                    new DeliveryOutcome(providerRequestId, ProviderDeliveryStatus.REJECTED, "COURSE_REJECTED"));
            case COMMIT_THEN_DELAY -> new DeliveryFact(
                    scenario.scenario(),
                    scenario.delay(),
                    1,
                    new DeliveryOutcome(providerRequestId, ProviderDeliveryStatus.SUCCEEDED, null));
        };
    }

    private Duration resolveDelay(ProviderScenario scenario, Duration requestedDelay) {
        if (scenario != ProviderScenario.COMMIT_THEN_DELAY) {
            return Duration.ZERO;
        }
        Duration delay = requestedDelay == null ? properties.defaultCommitDelay() : requestedDelay;
        if (delay.isNegative() || delay.isZero()) {
            throw new IllegalArgumentException("delayMillis must be positive for COMMIT_THEN_DELAY");
        }
        if (delay.compareTo(properties.maximumCommitDelay()) > 0) {
            throw new IllegalArgumentException("delayMillis exceeds configured maximum");
        }
        return delay;
    }

    private static void delay(Duration duration) {
        try {
            Thread.sleep(duration);
        } catch (InterruptedException interrupted) {
            Thread.currentThread().interrupt();
            throw new ProviderDelayInterruptedException(interrupted);
        }
    }

    private static String stableProviderRequestId(String idempotencyKey) {
        UUID value = UUID.nameUUIDFromBytes(("notifyflow-provider:" + idempotencyKey)
                .getBytes(StandardCharsets.UTF_8));
        return "provider-" + value;
    }

    private static void requireKey(String idempotencyKey) {
        if (idempotencyKey == null || idempotencyKey.isBlank()) {
            throw new IllegalArgumentException("Idempotency-Key is required");
        }
    }

    public record DeliveryRequest(
            String recipientRef,
            String templateCode,
            Map<String, Object> variables) {
    }

    public record DeliveryOutcome(
            String providerRequestId,
            ProviderDeliveryStatus status,
            String errorCode) {
    }

    public record ScenarioView(
            String idempotencyKey,
            ProviderScenario scenario,
            long delayMillis) {
    }

    public record EffectsView(
            String idempotencyKey,
            int sideEffectCount,
            String providerRequestId,
            ProviderDeliveryStatus status) {
    }

    private record ScenarioConfiguration(ProviderScenario scenario, Duration delay) {
    }

    private record DeliveryFact(
            ProviderScenario scenario,
            Duration delay,
            int sideEffectCount,
            DeliveryOutcome outcome) {
    }

    public static final class ScenarioAlreadyAppliedException extends RuntimeException {
        public ScenarioAlreadyAppliedException(String idempotencyKey) {
            super("delivery already exists for idempotency key: " + idempotencyKey);
        }
    }

    public static final class ProviderDelayInterruptedException extends RuntimeException {
        public ProviderDelayInterruptedException(InterruptedException cause) {
            super("provider response delay was interrupted", cause);
        }
    }
}
