package com.notifyflow.observability;

import java.util.ArrayList;
import java.util.List;
import java.util.PriorityQueue;

public final class LoadModelSimulator {

    private LoadModelSimulator() {
    }

    public static ClosedResult simulateClosed(
            long durationMillis,
            long slowdownAtMillis,
            int virtualUsers,
            long fastLatencyMillis,
            long slowLatencyMillis
    ) {
        validateCommon(durationMillis, slowdownAtMillis, fastLatencyMillis, slowLatencyMillis);
        if (virtualUsers <= 0) {
            throw new IllegalArgumentException("virtualUsers must be positive");
        }

        long fastStarted = 0L;
        long slowStarted = 0L;
        List<Long> observedLatencies = new ArrayList<>();

        for (int user = 0; user < virtualUsers; user++) {
            long nextStartMillis = 0L;
            while (nextStartMillis < durationMillis) {
                long serviceMillis;
                if (nextStartMillis < slowdownAtMillis) {
                    fastStarted++;
                    serviceMillis = fastLatencyMillis;
                } else {
                    slowStarted++;
                    serviceMillis = slowLatencyMillis;
                }
                observedLatencies.add(serviceMillis);
                nextStartMillis = Math.addExact(nextStartMillis, serviceMillis);
            }
        }

        return new ClosedResult(
                fastStarted,
                slowStarted,
                phaseRate(fastStarted, slowdownAtMillis),
                phaseRate(slowStarted, durationMillis - slowdownAtMillis),
                LatencyStatistics.summarize(toLongArray(observedLatencies))
        );
    }

    public static OpenResult simulateOpen(
            long durationMillis,
            long slowdownAtMillis,
            int arrivalRatePerSecond,
            long fastLatencyMillis,
            long slowLatencyMillis,
            int maxInFlight
    ) {
        validateCommon(durationMillis, slowdownAtMillis, fastLatencyMillis, slowLatencyMillis);
        if (arrivalRatePerSecond <= 0) {
            throw new IllegalArgumentException("arrivalRatePerSecond must be positive");
        }
        if (maxInFlight <= 0) {
            throw new IllegalArgumentException("maxInFlight must be positive");
        }

        long fastScheduled = 0L;
        long slowScheduled = 0L;
        long fastAccepted = 0L;
        long slowAccepted = 0L;
        long fastDropped = 0L;
        long slowDropped = 0L;
        List<Long> observedLatencies = new ArrayList<>();
        PriorityQueue<Double> completionTimes = new PriorityQueue<>();

        for (long sequence = 0L; ; sequence++) {
            double scheduledMillis = sequence * 1_000.0D / arrivalRatePerSecond;
            if (scheduledMillis >= durationMillis) {
                break;
            }

            while (!completionTimes.isEmpty()
                    && completionTimes.peek() <= scheduledMillis) {
                completionTimes.remove();
            }

            boolean fastPhase = scheduledMillis < slowdownAtMillis;
            long serviceMillis = fastPhase ? fastLatencyMillis : slowLatencyMillis;
            if (fastPhase) {
                fastScheduled++;
            } else {
                slowScheduled++;
            }

            if (completionTimes.size() >= maxInFlight) {
                if (fastPhase) {
                    fastDropped++;
                } else {
                    slowDropped++;
                }
                continue;
            }

            completionTimes.add(scheduledMillis + serviceMillis);
            observedLatencies.add(serviceMillis);
            if (fastPhase) {
                fastAccepted++;
            } else {
                slowAccepted++;
            }
        }

        return new OpenResult(
                fastScheduled,
                slowScheduled,
                fastAccepted,
                slowAccepted,
                fastDropped,
                slowDropped,
                LatencyStatistics.summarize(toLongArray(observedLatencies))
        );
    }

    private static void validateCommon(
            long durationMillis,
            long slowdownAtMillis,
            long fastLatencyMillis,
            long slowLatencyMillis
    ) {
        if (durationMillis <= 0L) {
            throw new IllegalArgumentException("durationMillis must be positive");
        }
        if (slowdownAtMillis <= 0L || slowdownAtMillis >= durationMillis) {
            throw new IllegalArgumentException("slowdownAtMillis must be inside the run");
        }
        if (fastLatencyMillis <= 0L || slowLatencyMillis <= 0L) {
            throw new IllegalArgumentException("service latency must be positive");
        }
    }

    private static double phaseRate(long count, long phaseDurationMillis) {
        return count * 1_000.0D / phaseDurationMillis;
    }

    private static long[] toLongArray(List<Long> values) {
        long[] result = new long[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index);
        }
        return result;
    }

    public record ClosedResult(
            long fastPhaseStarted,
            long slowPhaseStarted,
            double fastPhaseRatePerSecond,
            double slowPhaseRatePerSecond,
            LatencyStatistics.Summary observedLatency
    ) {
        public long totalStarted() {
            return fastPhaseStarted + slowPhaseStarted;
        }

        public double slowPhaseSampleRatio() {
            return (double) slowPhaseStarted / totalStarted();
        }
    }

    public record OpenResult(
            long fastPhaseScheduled,
            long slowPhaseScheduled,
            long fastPhaseAccepted,
            long slowPhaseAccepted,
            long fastPhaseDropped,
            long slowPhaseDropped,
            LatencyStatistics.Summary observedLatency
    ) {
        public long totalScheduled() {
            return fastPhaseScheduled + slowPhaseScheduled;
        }

        public long totalAccepted() {
            return fastPhaseAccepted + slowPhaseAccepted;
        }

        public long totalDropped() {
            return fastPhaseDropped + slowPhaseDropped;
        }

        public double slowPhaseDropRatio() {
            return (double) slowPhaseDropped / slowPhaseScheduled;
        }
    }
}
