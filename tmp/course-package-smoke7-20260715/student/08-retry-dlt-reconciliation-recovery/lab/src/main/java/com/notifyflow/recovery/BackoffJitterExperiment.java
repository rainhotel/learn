package com.notifyflow.recovery;

import java.util.Comparator;

public final class BackoffJitterExperiment {

    private BackoffJitterExperiment() {
    }

    public static void main(String[] args) {
        int tasks = 10_000;
        long capMillis = 1_000L;
        long bucketSizeMillis = 100L;
        long seed = 20_260_714L;

        BackoffSimulator.Distribution fixed = BackoffSimulator.simulateFixed(
                tasks,
                capMillis,
                bucketSizeMillis
        );
        BackoffSimulator.Distribution jitter = BackoffSimulator.simulateFullJitter(
                tasks,
                capMillis,
                bucketSizeMillis,
                seed
        );

        double peakRatio = (double) jitter.peakBucketCount() / fixed.peakBucketCount();

        System.out.println("tasks=" + tasks);
        System.out.println("cap_millis=" + capMillis);
        System.out.println("bucket_millis=" + bucketSizeMillis);
        System.out.println("fixed_peak=" + fixed.peakBucketCount());
        System.out.println("full_jitter_peak=" + jitter.peakBucketCount());
        System.out.printf("jitter_to_fixed_peak_ratio=%.4f%n", peakRatio);
        System.out.println("full_jitter_histogram:");
        jitter.bucketCounts().entrySet().stream()
                .sorted(Comparator.comparingInt(entry -> entry.getKey()))
                .forEach(entry -> System.out.println(
                        "  " + (entry.getKey() * bucketSizeMillis)
                                + "-" + ((entry.getKey() + 1L) * bucketSizeMillis - 1L)
                                + "ms=" + entry.getValue()
                ));

        if (jitter.peakBucketCount() >= fixed.peakBucketCount() / 5) {
            throw new AssertionError("full jitter did not reduce the peak enough");
        }

        System.out.println("BACKOFF_JITTER_EXPERIMENT_PASSED");
    }
}

