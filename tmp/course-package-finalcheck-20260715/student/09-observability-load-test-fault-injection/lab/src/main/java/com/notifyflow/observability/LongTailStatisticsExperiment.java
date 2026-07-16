package com.notifyflow.observability;

import java.util.Arrays;
import java.util.Locale;

public final class LongTailStatisticsExperiment {

    private LongTailStatisticsExperiment() {
    }

    public static long[] notifyFlowLatencySamples() {
        long[] samples = new long[10_000];
        Arrays.fill(samples, 0, 9_900, 10L);
        Arrays.fill(samples, 9_900, 9_999, 500L);
        samples[9_999] = 10_000L;
        return samples;
    }

    public static void main(String[] args) {
        LatencyStatistics.Summary summary = LatencyStatistics.summarize(
                notifyFlowLatencySamples()
        );
        double maxToAverageRatio = summary.maxMillis() / summary.averageMillis();

        System.out.println("sample_count=" + summary.sampleCount());
        System.out.printf(Locale.ROOT, "average_millis=%.4f%n", summary.averageMillis());
        System.out.println("p50_millis=" + summary.p50Millis());
        System.out.println("p95_millis=" + summary.p95Millis());
        System.out.println("p99_millis=" + summary.p99Millis());
        System.out.println("p999_millis=" + summary.p999Millis());
        System.out.println("max_millis=" + summary.maxMillis());
        System.out.printf(Locale.ROOT, "max_to_average_ratio=%.4f%n", maxToAverageRatio);

        if (summary.sampleCount() != 10_000L
                || Math.abs(summary.averageMillis() - 15.85D) > 0.0001D
                || summary.p99Millis() != 10L
                || summary.p999Millis() != 500L
                || summary.maxMillis() != 10_000L
                || maxToAverageRatio <= 600.0D) {
            throw new AssertionError("long-tail statistics do not match the experiment fixture");
        }

        System.out.println("LONG_TAIL_STATISTICS_EXPERIMENT_PASSED");
    }
}
