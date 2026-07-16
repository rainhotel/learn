package com.notifyflow.observability;

import java.util.Locale;

public final class MetricCardinalityExperiment {

    private MetricCardinalityExperiment() {
    }

    public static void main(String[] args) {
        String[] providers = {"aliyun", "tencent", "mock"};
        String[] results = {"success", "failure"};
        MetricCardinalitySimulator.Result safe = MetricCardinalitySimulator.simulate(
                10_000,
                providers,
                results,
                false
        );
        MetricCardinalitySimulator.Result dangerous = MetricCardinalitySimulator.simulate(
                10_000,
                providers,
                results,
                true
        );
        double amplification = (double) dangerous.uniqueSeries() / safe.uniqueSeries();
        long assumedBytesPerSeries = 512L;
        long estimatedBytes = dangerous.estimatedMetadataBytes(assumedBytesPerSeries);
        double estimatedMebibytes = estimatedBytes / 1_048_576.0D;

        System.out.println("events=" + safe.eventCount());
        System.out.println("low_cardinality_series=" + safe.uniqueSeries());
        System.out.println("task_id_series=" + dangerous.uniqueSeries());
        System.out.printf(Locale.ROOT, "series_amplification=%.4f%n", amplification);
        System.out.println("assumed_metadata_bytes_per_series=" + assumedBytesPerSeries);
        System.out.println("estimated_metadata_bytes=" + estimatedBytes);
        System.out.printf(Locale.ROOT, "estimated_metadata_mib=%.4f%n", estimatedMebibytes);

        if (safe.uniqueSeries() != 6L
                || dangerous.uniqueSeries() != 10_000L
                || amplification <= 1_000.0D
                || !MetricCardinalitySimulator.isForbiddenTagKey("taskId")
                || MetricCardinalitySimulator.isForbiddenTagKey("provider")) {
            throw new AssertionError("metric cardinality result does not match the fixture");
        }

        System.out.println("METRIC_CARDINALITY_EXPERIMENT_PASSED");
    }
}
