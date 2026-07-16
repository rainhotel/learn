package com.notifyflow.observability;

public final class ObservabilityExperimentsTest {

    public static void main(String[] args) {
        testLongTailStatisticsExposeHiddenLatency();
        testInvalidPercentileIsRejected();
        testClosedLoadCollapsesWhenServiceSlows();
        testOpenLoadPreservesScheduledArrivalRateAndExposesDrops();
        testCoordinatedOmissionHidesSlowPhaseFromClosedModelP95();
        testLowCardinalityTagsBoundSeriesCount();
        testTaskIdCreatesLinearSeriesGrowth();
        testDangerousMetricTagKeysAreRejected();
        testThreadPoolBelowCapacityHasNoRejection();
        testThreadPoolOverloadFillsQueueAndRejects();
        testThreadPoolConfigurationIsValidated();
        System.out.println("ALL_OBSERVABILITY_EXPERIMENT_TESTS_PASSED");
    }

    private static void testLongTailStatisticsExposeHiddenLatency() {
        long[] samples = LongTailStatisticsExperiment.notifyFlowLatencySamples();
        LatencyStatistics.Summary summary = LatencyStatistics.summarize(samples);

        assertApproximately(15.85D, summary.averageMillis(), 0.0001D, "average latency");
        assertEquals(10L, summary.p50Millis(), "P50 latency");
        assertEquals(10L, summary.p95Millis(), "P95 latency");
        assertEquals(10L, summary.p99Millis(), "P99 latency");
        assertEquals(500L, summary.p999Millis(), "P99.9 latency");
        assertEquals(10_000L, summary.maxMillis(), "max latency");
    }

    private static void testInvalidPercentileIsRejected() {
        assertThrows(
                IllegalArgumentException.class,
                () -> LatencyStatistics.percentile(new long[]{10L}, 0.0D),
                "zero percentile must be rejected"
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> LatencyStatistics.summarize(new long[0]),
                "empty samples must be rejected"
        );
    }

    private static void testClosedLoadCollapsesWhenServiceSlows() {
        LoadModelSimulator.ClosedResult result = LoadModelSimulator.simulateClosed(
                60_000L,
                30_000L,
                100,
                100L,
                2_000L
        );

        assertEquals(30_000L, result.fastPhaseStarted(), "closed fast-phase requests");
        assertEquals(1_500L, result.slowPhaseStarted(), "closed slow-phase requests");
        assertTrue(
                result.slowPhaseRatePerSecond() < result.fastPhaseRatePerSecond() / 10.0D,
                "closed model arrival rate must collapse as latency rises"
        );
    }

    private static void testOpenLoadPreservesScheduledArrivalRateAndExposesDrops() {
        LoadModelSimulator.OpenResult result = LoadModelSimulator.simulateOpen(
                60_000L,
                30_000L,
                500,
                100L,
                2_000L,
                300
        );

        assertEquals(15_000L, result.fastPhaseScheduled(), "open fast-phase scheduled arrivals");
        assertEquals(15_000L, result.slowPhaseScheduled(), "open slow-phase scheduled arrivals");
        assertEquals(0L, result.fastPhaseDropped(), "fast phase must not drop arrivals");
        assertTrue(result.slowPhaseDropped() > 0L, "slow phase must expose insufficient capacity");
        assertEquals(30_000L, result.totalScheduled(), "open total scheduled arrivals");
    }

    private static void testCoordinatedOmissionHidesSlowPhaseFromClosedModelP95() {
        LoadModelSimulator.ClosedResult closed = LoadModelSimulator.simulateClosed(
                60_000L,
                30_000L,
                100,
                100L,
                2_000L
        );
        LoadModelSimulator.OpenResult open = LoadModelSimulator.simulateOpen(
                60_000L,
                30_000L,
                500,
                100L,
                2_000L,
                300
        );

        assertEquals(100L, closed.observedLatency().p95Millis(), "closed-model P95");
        assertEquals(2_000L, open.observedLatency().p95Millis(), "open-model P95");
        assertTrue(
                closed.slowPhaseSampleRatio() < 0.05D,
                "closed observations must under-sample the slow half of wall-clock time"
        );
    }

    private static void testLowCardinalityTagsBoundSeriesCount() {
        MetricCardinalitySimulator.Result result = MetricCardinalitySimulator.simulate(
                10_000,
                new String[]{"aliyun", "tencent", "mock"},
                new String[]{"success", "failure"},
                false
        );

        assertEquals(6L, result.uniqueSeries(), "provider/result series count");
    }

    private static void testTaskIdCreatesLinearSeriesGrowth() {
        MetricCardinalitySimulator.Result safe = MetricCardinalitySimulator.simulate(
                10_000,
                new String[]{"aliyun", "tencent", "mock"},
                new String[]{"success", "failure"},
                false
        );
        MetricCardinalitySimulator.Result dangerous = MetricCardinalitySimulator.simulate(
                10_000,
                new String[]{"aliyun", "tencent", "mock"},
                new String[]{"success", "failure"},
                true
        );

        assertEquals(10_000L, dangerous.uniqueSeries(), "taskId series count");
        assertTrue(
                dangerous.uniqueSeries() > safe.uniqueSeries() * 1_000L,
                "taskId must demonstrate cardinality amplification"
        );
        assertEquals(
                dangerous.uniqueSeries() * 512L,
                dangerous.estimatedMetadataBytes(512L),
                "explicit metadata estimate"
        );
    }

    private static void testDangerousMetricTagKeysAreRejected() {
        assertTrue(MetricCardinalitySimulator.isForbiddenTagKey("taskId"), "taskId tag");
        assertTrue(MetricCardinalitySimulator.isForbiddenTagKey("trace_id"), "trace_id tag");
        assertTrue(MetricCardinalitySimulator.isForbiddenTagKey("user-id"), "user-id tag");
        assertFalse(MetricCardinalitySimulator.isForbiddenTagKey("provider"), "provider tag");
        assertFalse(MetricCardinalitySimulator.isForbiddenTagKey("result"), "result tag");
    }

    private static void testThreadPoolBelowCapacityHasNoRejection() {
        ThreadPoolSaturationSimulator.Result result = ThreadPoolSaturationSimulator.simulate(
                60_000L,
                100,
                20,
                100,
                100L
        );

        assertEquals(6_000L, result.offered());
        assertEquals(0L, result.rejected(), "below-capacity rejection count");
        assertEquals(0L, result.maxQueueDepth(), "below-capacity max queue");
        assertEquals(100L, result.completedLatency().p99Millis(), "below-capacity P99");
    }

    private static void testThreadPoolOverloadFillsQueueAndRejects() {
        ThreadPoolSaturationSimulator.Result result = ThreadPoolSaturationSimulator.simulate(
                60_000L,
                250,
                20,
                100,
                100L
        );

        assertEquals(15_000L, result.offered(), "overload offered requests");
        assertEquals(100L, result.maxQueueDepth(), "overload max queue");
        assertTrue(result.rejected() > 0L, "overload must reject requests");
        assertTrue(
                result.completedThroughputPerSecond() <= 200.0D,
                "completed throughput must not exceed worker service capacity"
        );
        assertTrue(
                result.completedLatency().p99Millis() >= 500L,
                "queueing must increase P99"
        );
    }

    private static void testThreadPoolConfigurationIsValidated() {
        assertThrows(
                IllegalArgumentException.class,
                () -> ThreadPoolSaturationSimulator.simulate(60_000L, 250, 0, 100, 100L),
                "zero workers must be rejected"
        );
        assertThrows(
                IllegalArgumentException.class,
                () -> ThreadPoolSaturationSimulator.simulate(60_000L, 333, 20, 100, 100L),
                "non-integral microsecond arrival interval must be rejected"
        );
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertEquals(long expected, long actual) {
        assertEquals(expected, actual, "long value");
    }

    private static void assertApproximately(
            double expected,
            double actual,
            double tolerance,
            String message
    ) {
        if (Math.abs(expected - actual) > tolerance) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static void assertFalse(boolean condition, String message) {
        assertTrue(!condition, message);
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
