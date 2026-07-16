package com.notifyflow.observability;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.PriorityQueue;

public final class ThreadPoolSaturationSimulator {

    private static final long MICROS_PER_SECOND = 1_000_000L;
    private static final long MICROS_PER_MILLI = 1_000L;

    private ThreadPoolSaturationSimulator() {
    }

    public static Result simulate(
            long durationMillis,
            int arrivalRatePerSecond,
            int workers,
            int queueCapacity,
            long serviceMillis
    ) {
        validate(durationMillis, arrivalRatePerSecond, workers, queueCapacity, serviceMillis);

        long durationMicros = Math.multiplyExact(durationMillis, MICROS_PER_MILLI);
        long serviceMicros = Math.multiplyExact(serviceMillis, MICROS_PER_MILLI);
        long arrivalIntervalMicros = MICROS_PER_SECOND / arrivalRatePerSecond;
        SimulationState state = new SimulationState(workers, queueCapacity, serviceMicros);

        for (long arrivalMicros = 0L;
             arrivalMicros < durationMicros;
             arrivalMicros += arrivalIntervalMicros) {
            state.completeUntil(arrivalMicros);
            state.offer(arrivalMicros);
        }
        state.completeUntil(durationMicros);

        return new Result(
                state.offered,
                state.offered - state.rejected,
                state.rejected,
                state.completed,
                state.maxActive,
                state.maxQueueDepth,
                state.queue.size(),
                LatencyStatistics.summarize(toLongArray(state.completedLatenciesMillis)),
                state.completed * 1_000.0D / durationMillis
        );
    }

    private static void validate(
            long durationMillis,
            int arrivalRatePerSecond,
            int workers,
            int queueCapacity,
            long serviceMillis
    ) {
        if (durationMillis <= 0L || serviceMillis <= 0L) {
            throw new IllegalArgumentException("duration and service time must be positive");
        }
        if (durationMillis < serviceMillis) {
            throw new IllegalArgumentException("duration must allow at least one completion");
        }
        if (arrivalRatePerSecond <= 0
                || MICROS_PER_SECOND % arrivalRatePerSecond != 0L) {
            throw new IllegalArgumentException(
                    "arrival rate must produce an integral microsecond interval"
            );
        }
        if (workers <= 0) {
            throw new IllegalArgumentException("workers must be positive");
        }
        if (queueCapacity < 0) {
            throw new IllegalArgumentException("queueCapacity must not be negative");
        }
    }

    private static long[] toLongArray(List<Long> values) {
        long[] result = new long[values.size()];
        for (int index = 0; index < values.size(); index++) {
            result[index] = values.get(index);
        }
        return result;
    }

    private record InFlight(long completionMicros, long arrivalMicros) {
    }

    private static final class SimulationState {

        private final int workers;
        private final int queueCapacity;
        private final long serviceMicros;
        private final PriorityQueue<InFlight> inFlight = new PriorityQueue<>(
                Comparator.comparingLong(InFlight::completionMicros)
        );
        private final ArrayDeque<Long> queue = new ArrayDeque<>();
        private final List<Long> completedLatenciesMillis = new ArrayList<>();

        private long offered;
        private long rejected;
        private long completed;
        private int maxActive;
        private int maxQueueDepth;

        private SimulationState(int workers, int queueCapacity, long serviceMicros) {
            this.workers = workers;
            this.queueCapacity = queueCapacity;
            this.serviceMicros = serviceMicros;
        }

        private void offer(long arrivalMicros) {
            offered++;
            if (inFlight.size() < workers) {
                start(arrivalMicros, arrivalMicros);
                return;
            }
            if (queue.size() < queueCapacity) {
                queue.addLast(arrivalMicros);
                maxQueueDepth = Math.max(maxQueueDepth, queue.size());
                return;
            }
            rejected++;
        }

        private void completeUntil(long timeMicros) {
            while (!inFlight.isEmpty()
                    && inFlight.peek().completionMicros() <= timeMicros) {
                InFlight finished = inFlight.remove();
                completed++;
                completedLatenciesMillis.add(
                        (finished.completionMicros() - finished.arrivalMicros())
                                / MICROS_PER_MILLI
                );
                if (!queue.isEmpty()) {
                    long queuedArrivalMicros = queue.removeFirst();
                    start(queuedArrivalMicros, finished.completionMicros());
                }
            }
        }

        private void start(long arrivalMicros, long startMicros) {
            inFlight.add(new InFlight(
                    Math.addExact(startMicros, serviceMicros),
                    arrivalMicros
            ));
            maxActive = Math.max(maxActive, inFlight.size());
        }
    }

    public record Result(
            long offered,
            long accepted,
            long rejected,
            long completed,
            int maxActive,
            int maxQueueDepth,
            int queueDepthAtEnd,
            LatencyStatistics.Summary completedLatency,
            double completedThroughputPerSecond
    ) {
        public double rejectionRatio() {
            return (double) rejected / offered;
        }
    }
}
