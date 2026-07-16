package com.notifyflow.recovery;

public final class RecoveryExperimentsTest {

    public static void main(String[] args) {
        testFiveLayersWithThreeAttemptsAmplifiesTo243();
        testSingleRetryOwnerLimitsCallsToThree();
        testInvalidRetryConfigurationIsRejected();
        testCappedExponentialDelay();
        testFullJitterSpreadsRetryPeak();
        testJitterSimulationIsDeterministicForSeed();
        System.out.println("ALL_RECOVERY_EXPERIMENT_TESTS_PASSED");
    }

    private static void testFiveLayersWithThreeAttemptsAmplifiesTo243() {
        long calls = RetryAmplificationSimulator.countLeafCalls(5, 3);
        assertEquals(243L, calls, "five layers with three attempts");
    }

    private static void testSingleRetryOwnerLimitsCallsToThree() {
        long calls = RetryAmplificationSimulator.countLeafCalls(1, 3);
        assertEquals(3L, calls, "single retry owner");
    }

    private static void testInvalidRetryConfigurationIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> RetryAmplificationSimulator.countLeafCalls(0, 3),
                "zero layers must be rejected"
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> RetryAmplificationSimulator.countLeafCalls(2, 0),
                "zero attempts must be rejected"
        );
    }

    private static void testCappedExponentialDelay() {
        assertEquals(
                8_000L,
                BackoffSimulator.cappedExponentialDelay(1_000L, 30_000L, 4),
                "attempt four delay"
        );
        assertEquals(
                30_000L,
                BackoffSimulator.cappedExponentialDelay(1_000L, 30_000L, 10),
                "delay must be capped"
        );
    }

    private static void testFullJitterSpreadsRetryPeak() {
        int tasks = 10_000;
        BackoffSimulator.Distribution fixed = BackoffSimulator.simulateFixed(
                tasks,
                1_000L,
                100L
        );
        BackoffSimulator.Distribution jitter = BackoffSimulator.simulateFullJitter(
                tasks,
                1_000L,
                100L,
                20_260_714L
        );

        assertEquals(tasks, fixed.peakBucketCount(), "fixed retry peak");
        assertTrue(
                jitter.peakBucketCount() < fixed.peakBucketCount() / 5,
                "full jitter peak must be below 20% of the fixed peak"
        );
        assertEquals(tasks, jitter.totalSamples(), "jitter sample count");
    }

    private static void testJitterSimulationIsDeterministicForSeed() {
        BackoffSimulator.Distribution first = BackoffSimulator.simulateFullJitter(
                1_000,
                1_000L,
                100L,
                42L
        );
        BackoffSimulator.Distribution second = BackoffSimulator.simulateFullJitter(
                1_000,
                1_000L,
                100L,
                42L
        );
        assertEquals(first.bucketCounts(), second.bucketCounts(), "seeded distribution");
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertEquals(Object expected, Object actual, String message) {
        if (!expected.equals(actual)) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertThrows(
            Class<? extends Throwable> expectedType,
            Runnable action,
            String message
    ) {
        try {
            action.run();
        } catch (Throwable throwable) {
            if (expectedType.isInstance(throwable)) {
                return;
            }
            throw new AssertionError(
                    message + ": expected=" + expectedType.getName()
                            + ", actual=" + throwable.getClass().getName(),
                    throwable
            );
        }
        throw new AssertionError(message + ": expected exception " + expectedType.getName());
    }
}

