package com.notifyflow.observability;

import java.util.Locale;

public final class OpenClosedLoadExperiment {

    private OpenClosedLoadExperiment() {
    }

    public static void main(String[] args) {
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
        long omittedSlowPhaseDemand = open.slowPhaseScheduled() - closed.slowPhaseStarted();

        System.out.println("closed_fast_started=" + closed.fastPhaseStarted());
        System.out.println("closed_slow_started=" + closed.slowPhaseStarted());
        System.out.printf(
                Locale.ROOT,
                "closed_fast_rate_per_second=%.2f%n",
                closed.fastPhaseRatePerSecond()
        );
        System.out.printf(
                Locale.ROOT,
                "closed_slow_rate_per_second=%.2f%n",
                closed.slowPhaseRatePerSecond()
        );
        System.out.println("closed_observed_p95_millis=" + closed.observedLatency().p95Millis());
        System.out.println("open_fast_scheduled=" + open.fastPhaseScheduled());
        System.out.println("open_slow_scheduled=" + open.slowPhaseScheduled());
        System.out.println("open_slow_accepted=" + open.slowPhaseAccepted());
        System.out.println("open_slow_dropped=" + open.slowPhaseDropped());
        System.out.printf(
                Locale.ROOT,
                "open_slow_drop_ratio=%.4f%n",
                open.slowPhaseDropRatio()
        );
        System.out.println("open_observed_p95_millis=" + open.observedLatency().p95Millis());
        System.out.println("closed_omitted_slow_phase_demand=" + omittedSlowPhaseDemand);

        if (closed.fastPhaseStarted() != 30_000L
                || closed.slowPhaseStarted() != 1_500L
                || open.fastPhaseScheduled() != 15_000L
                || open.slowPhaseScheduled() != 15_000L
                || open.fastPhaseDropped() != 0L
                || open.slowPhaseDropped() <= 0L
                || closed.observedLatency().p95Millis() != 100L
                || open.observedLatency().p95Millis() != 2_000L
                || omittedSlowPhaseDemand != 13_500L) {
            throw new AssertionError("open/closed load model result does not match the fixture");
        }

        System.out.println("OPEN_CLOSED_LOAD_EXPERIMENT_PASSED");
    }
}
