package dev.notifyflow.course.providerstub;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.time.Duration;
import java.util.Map;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import dev.notifyflow.course.providerstub.ProviderStubService.DeliveryRequest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

class ProviderStubServiceTest {

    private final ProviderStubService service = new ProviderStubService(
            new ProviderStubProperties(Duration.ofMillis(20), Duration.ofSeconds(1)));

    @AfterEach
    void clearState() {
        service.clearAllForTest();
    }

    @Test
    void repeatedSuccessUsesOneSideEffectAndStableProviderRequestId() {
        var first = service.deliver("attempt-1", request());
        var replay = service.deliver("attempt-1", request());

        assertThat(first.status()).isEqualTo(ProviderDeliveryStatus.SUCCEEDED);
        assertThat(replay).isEqualTo(first);
        assertThat(service.effects("attempt-1").sideEffectCount()).isEqualTo(1);
    }

    @Test
    void concurrentDuplicatesStillProduceOneVisibleSideEffect() throws Exception {
        int calls = 16;
        ExecutorService pool = Executors.newFixedThreadPool(calls);
        CountDownLatch ready = new CountDownLatch(calls);
        CountDownLatch start = new CountDownLatch(1);
        try {
            for (int index = 0; index < calls; index++) {
                pool.submit(() -> {
                    ready.countDown();
                    start.await();
                    service.deliver("attempt-concurrent", request());
                    return null;
                });
            }
            ready.await();
            start.countDown();
        } finally {
            pool.shutdown();
            assertThat(pool.awaitTermination(5, java.util.concurrent.TimeUnit.SECONDS)).isTrue();
        }

        assertThat(service.effects("attempt-concurrent").sideEffectCount()).isEqualTo(1);
    }

    @Test
    void rejectHasNoVisibleSideEffectAndIsQueryable() {
        service.configureScenario("attempt-reject", ProviderScenario.REJECT, null);

        var result = service.deliver("attempt-reject", request());

        assertThat(result.status()).isEqualTo(ProviderDeliveryStatus.REJECTED);
        assertThat(service.query("attempt-reject").status()).isEqualTo(ProviderDeliveryStatus.REJECTED);
        assertThat(service.effects("attempt-reject").sideEffectCount()).isZero();
    }

    @Test
    void commitThenDelayPersistsSuccessBeforeReturning() {
        service.configureScenario(
                "attempt-delay",
                ProviderScenario.COMMIT_THEN_DELAY,
                Duration.ofMillis(25));

        long startedAt = System.nanoTime();
        var result = service.deliver("attempt-delay", request());
        long elapsedMillis = Duration.ofNanos(System.nanoTime() - startedAt).toMillis();

        assertThat(elapsedMillis).isGreaterThanOrEqualTo(20);
        assertThat(result.status()).isEqualTo(ProviderDeliveryStatus.SUCCEEDED);
        assertThat(service.query("attempt-delay").status()).isEqualTo(ProviderDeliveryStatus.SUCCEEDED);
        assertThat(service.effects("attempt-delay").sideEffectCount()).isEqualTo(1);
    }

    @Test
    void scenarioCannotChangeAfterDeliveryFactExists() {
        service.deliver("attempt-fixed", request());

        assertThatThrownBy(() -> service.configureScenario(
                "attempt-fixed",
                ProviderScenario.REJECT,
                null))
                .isInstanceOf(ProviderStubService.ScenarioAlreadyAppliedException.class);
    }

    @Test
    void queryUnknownKeyReturnsNotFoundWithoutCreatingEffect() {
        assertThat(service.query("missing").status()).isEqualTo(ProviderDeliveryStatus.NOT_FOUND);
        assertThat(service.effects("missing").sideEffectCount()).isZero();
    }

    private static DeliveryRequest request() {
        return new DeliveryRequest(
                "recipient-fixture-001",
                "WELCOME_V1",
                Map.of("name", "fixture-user"));
    }
}
