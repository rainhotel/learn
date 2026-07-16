package com.notifyflow.recovery;

import java.util.LinkedHashMap;
import java.util.Map;
import java.util.SplittableRandom;

public final class BackoffSimulator {

    private BackoffSimulator() {
    }

    public static long cappedExponentialDelay(
            long baseDelayMillis,
            long maxDelayMillis,
            int attemptNumber
    ) {
        if (baseDelayMillis < 1L) {
            throw new IllegalArgumentException("baseDelayMillis must be positive");
        }
        if (maxDelayMillis < baseDelayMillis) {
            throw new IllegalArgumentException("maxDelayMillis must be >= baseDelayMillis");
        }
        if (attemptNumber < 1) {
            throw new IllegalArgumentException("attemptNumber must be at least 1");
        }

        long delay = baseDelayMillis;
        for (int attempt = 1; attempt < attemptNumber; attempt++) {
            if (delay >= maxDelayMillis / 2L) {
                return maxDelayMillis;
            }
            delay *= 2L;
        }
        return Math.min(delay, maxDelayMillis);
    }

    public static Distribution simulateFixed(
            int tasks,
            long delayMillis,
            long bucketSizeMillis
    ) {
        validateSimulation(tasks, delayMillis, bucketSizeMillis);
        int bucket = Math.toIntExact(delayMillis / bucketSizeMillis);
        return new Distribution(Map.of(bucket, tasks), tasks, tasks);
    }

    public static Distribution simulateFullJitter(
            int tasks,
            long capMillis,
            long bucketSizeMillis,
            long seed
    ) {
        validateSimulation(tasks, capMillis, bucketSizeMillis);
        SplittableRandom random = new SplittableRandom(seed);
        Map<Integer, Integer> buckets = new LinkedHashMap<>();
        int peak = 0;

        for (int task = 0; task < tasks; task++) {
            long delay = random.nextLong(capMillis + 1L);
            int bucket = Math.toIntExact(delay / bucketSizeMillis);
            int count = buckets.merge(bucket, 1, Integer::sum);
            peak = Math.max(peak, count);
        }

        return new Distribution(Map.copyOf(buckets), peak, tasks);
    }

    private static void validateSimulation(
            int tasks,
            long delayOrCapMillis,
            long bucketSizeMillis
    ) {
        if (tasks < 1) {
            throw new IllegalArgumentException("tasks must be positive");
        }
        if (delayOrCapMillis < 0L) {
            throw new IllegalArgumentException("delay/cap must not be negative");
        }
        if (bucketSizeMillis < 1L) {
            throw new IllegalArgumentException("bucketSizeMillis must be positive");
        }
    }

    public record Distribution(
            Map<Integer, Integer> bucketCounts,
            int peakBucketCount,
            int totalSamples
    ) {
    }
}

