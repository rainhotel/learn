package com.notifyflow.observability;

import java.util.Arrays;

public final class LatencyStatistics {

    private LatencyStatistics() {
    }

    public static Summary summarize(long[] samplesMillis) {
        validateSamples(samplesMillis);
        long[] sorted = Arrays.copyOf(samplesMillis, samplesMillis.length);
        Arrays.sort(sorted);

        long sum = 0L;
        for (long sample : sorted) {
            sum = Math.addExact(sum, sample);
        }

        return new Summary(
                sorted.length,
                (double) sum / sorted.length,
                percentileFromSorted(sorted, 0.50D),
                percentileFromSorted(sorted, 0.95D),
                percentileFromSorted(sorted, 0.99D),
                percentileFromSorted(sorted, 0.999D),
                sorted[sorted.length - 1]
        );
    }

    public static long percentile(long[] samplesMillis, double percentile) {
        validateSamples(samplesMillis);
        validatePercentile(percentile);
        long[] sorted = Arrays.copyOf(samplesMillis, samplesMillis.length);
        Arrays.sort(sorted);
        return percentileFromSorted(sorted, percentile);
    }

    private static long percentileFromSorted(long[] sorted, double percentile) {
        validatePercentile(percentile);
        int nearestRank = (int) Math.ceil(percentile * sorted.length);
        return sorted[nearestRank - 1];
    }

    private static void validateSamples(long[] samplesMillis) {
        if (samplesMillis == null || samplesMillis.length == 0) {
            throw new IllegalArgumentException("latency samples must not be empty");
        }
        for (long sample : samplesMillis) {
            if (sample < 0L) {
                throw new IllegalArgumentException("latency sample must not be negative");
            }
        }
    }

    private static void validatePercentile(double percentile) {
        if (!(percentile > 0.0D && percentile <= 1.0D)) {
            throw new IllegalArgumentException("percentile must be in (0, 1]");
        }
    }

    public record Summary(
            long sampleCount,
            double averageMillis,
            long p50Millis,
            long p95Millis,
            long p99Millis,
            long p999Millis,
            long maxMillis
    ) {
    }
}
