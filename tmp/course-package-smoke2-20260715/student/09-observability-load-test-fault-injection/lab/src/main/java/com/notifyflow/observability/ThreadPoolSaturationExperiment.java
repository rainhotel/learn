package com.notifyflow.observability;

import java.util.Locale;

public final class ThreadPoolSaturationExperiment {

    private static final int[] ARRIVAL_RATES = {100, 125, 200, 250, 400, 500};

    private ThreadPoolSaturationExperiment() {
    }

    public static void main(String[] args) {
        ThreadPoolSaturationSimulator.Result baseline = null;
        ThreadPoolSaturationSimulator.Result overload = null;

        System.out.println("rate,offered,accepted,rejected,max_queue,p99_ms,throughput_per_second");
        for (int rate : ARRIVAL_RATES) {
            ThreadPoolSaturationSimulator.Result result =
                    ThreadPoolSaturationSimulator.simulate(
                            60_000L,
                            rate,
                            20,
                            100,
                            100L
                    );
            System.out.printf(
                    Locale.ROOT,
                    "%d,%d,%d,%d,%d,%d,%.2f%n",
                    rate,
                    result.offered(),
                    result.accepted(),
                    result.rejected(),
                    result.maxQueueDepth(),
                    result.completedLatency().p99Millis(),
                    result.completedThroughputPerSecond()
            );
            if (rate == 100) {
                baseline = result;
            }
            if (rate == 250) {
                overload = result;
            }
        }

        if (baseline == null
                || overload == null
                || baseline.rejected() != 0L
                || baseline.maxQueueDepth() != 0
                || overload.rejected() <= 0L
                || overload.maxQueueDepth() != 100
                || overload.completedLatency().p99Millis() < 500L
                || overload.completedThroughputPerSecond() > 200.0D) {
            throw new AssertionError("thread-pool saturation result does not match the fixture");
        }

        System.out.println("THREAD_POOL_SATURATION_EXPERIMENT_PASSED");
    }
}
