package com.notifyflow.observability;

import jdk.jfr.Recording;
import jdk.jfr.consumer.RecordedEvent;
import jdk.jfr.consumer.RecordingFile;

import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class ThreadPoolJfrExperiment {

    private static final int WORKERS = 2;
    private static final int QUEUE_CAPACITY = 2;
    private static final int SUBMITTED_TASKS = 6;

    private ThreadPoolJfrExperiment() {
    }

    public static ExperimentResult run(Path recordingPath) throws Exception {
        if (recordingPath == null) {
            throw new IllegalArgumentException("recordingPath must not be null");
        }
        Path parent = recordingPath.toAbsolutePath().getParent();
        if (parent != null) {
            Files.createDirectories(parent);
        }

        ExecutionSummary execution;
        try (Recording recording = new Recording()) {
            recording.enable(ProviderCallJfrEvent.class)
                    .withThreshold(Duration.ZERO)
                    .withoutStackTrace();
            recording.setToDisk(true);
            recording.start();
            execution = executeDeterministicSaturation();
            recording.stop();
            recording.dump(recordingPath);
        }

        RecordingSummary recording = readRecording(recordingPath);
        return new ExperimentResult(execution, recording, Files.size(recordingPath));
    }

    private static ExecutionSummary executeDeterministicSaturation() throws Exception {
        CountDownLatch started = new CountDownLatch(WORKERS);
        CountDownLatch release = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(WORKERS + QUEUE_CAPACITY);
        AtomicInteger submitted = new AtomicInteger();
        AtomicInteger rejected = new AtomicInteger();
        AtomicInteger maxQueueDepth = new AtomicInteger();
        AtomicInteger threadNumber = new AtomicInteger();

        ThreadFactory threadFactory = runnable -> {
            Thread thread = new Thread(
                    runnable,
                    "notifyflow-provider-" + threadNumber.incrementAndGet()
            );
            thread.setDaemon(false);
            return thread;
        };
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                WORKERS,
                WORKERS,
                0L,
                TimeUnit.MILLISECONDS,
                new ArrayBlockingQueue<>(QUEUE_CAPACITY),
                threadFactory,
                new ThreadPoolExecutor.AbortPolicy()
        );

        try {
            submit(executor, started, release, done, submitted, rejected, maxQueueDepth);
            submit(executor, started, release, done, submitted, rejected, maxQueueDepth);
            if (!started.await(5L, TimeUnit.SECONDS)) {
                throw new IllegalStateException("workers did not start in time");
            }

            submit(executor, started, release, done, submitted, rejected, maxQueueDepth);
            submit(executor, started, release, done, submitted, rejected, maxQueueDepth);
            if (executor.getQueue().size() != QUEUE_CAPACITY) {
                throw new IllegalStateException("bounded queue was not filled deterministically");
            }

            submit(executor, started, release, done, submitted, rejected, maxQueueDepth);
            submit(executor, started, release, done, submitted, rejected, maxQueueDepth);
        } finally {
            release.countDown();
            executor.shutdown();
        }

        if (!done.await(5L, TimeUnit.SECONDS)) {
            throw new IllegalStateException("accepted tasks did not complete in time");
        }
        if (!executor.awaitTermination(5L, TimeUnit.SECONDS)) {
            throw new IllegalStateException("executor did not terminate in time");
        }

        long completed = executor.getCompletedTaskCount();
        long accepted = submitted.get() - rejected.get();
        return new ExecutionSummary(
                submitted.get(),
                accepted,
                rejected.get(),
                completed,
                maxQueueDepth.get(),
                WORKERS,
                QUEUE_CAPACITY
        );
    }

    private static void submit(
            ThreadPoolExecutor executor,
            CountDownLatch started,
            CountDownLatch release,
            CountDownLatch done,
            AtomicInteger submitted,
            AtomicInteger rejected,
            AtomicInteger maxQueueDepth
    ) {
        submitted.incrementAndGet();
        try {
            executor.execute(() -> runProviderTask(executor, started, release, done));
            maxQueueDepth.accumulateAndGet(executor.getQueue().size(), Math::max);
        } catch (RejectedExecutionException exception) {
            rejected.incrementAndGet();
            maxQueueDepth.accumulateAndGet(executor.getQueue().size(), Math::max);
        }
    }

    private static void runProviderTask(
            ThreadPoolExecutor executor,
            CountDownLatch started,
            CountDownLatch release,
            CountDownLatch done
    ) {
        ProviderCallJfrEvent event = new ProviderCallJfrEvent();
        event.taskType = "provider-call";
        event.accepted = true;
        event.queueDepth = executor.getQueue().size();
        event.outcome = "SUCCESS";
        long startedNanos = System.nanoTime();
        event.begin();
        started.countDown();

        try {
            release.await();
        } catch (InterruptedException exception) {
            Thread.currentThread().interrupt();
            event.outcome = "INTERRUPTED";
        } finally {
            event.durationNanos = Math.max(0L, System.nanoTime() - startedNanos);
            event.end();
            event.commit();
            done.countDown();
        }
    }

    private static RecordingSummary readRecording(Path recordingPath) throws Exception {
        long providerCallEvents = 0L;
        long successEvents = 0L;
        long minimumDurationNanos = Long.MAX_VALUE;
        long maximumDurationNanos = 0L;
        long totalDurationNanos = 0L;

        try (RecordingFile recordingFile = new RecordingFile(recordingPath)) {
            while (recordingFile.hasMoreEvents()) {
                RecordedEvent event = recordingFile.readEvent();
                if (!ProviderCallJfrEvent.EVENT_NAME.equals(
                        event.getEventType().getName()
                )) {
                    continue;
                }
                providerCallEvents++;
                if (event.getBoolean("accepted")
                        && "SUCCESS".equals(event.getString("outcome"))) {
                    successEvents++;
                }
                int queueDepth = event.getInt("queueDepth");
                if (queueDepth < 0 || queueDepth > QUEUE_CAPACITY) {
                    throw new IllegalStateException("JFR queue depth is outside experiment bounds");
                }
                long durationNanos = event.getLong("durationNanos");
                minimumDurationNanos = Math.min(minimumDurationNanos, durationNanos);
                maximumDurationNanos = Math.max(maximumDurationNanos, durationNanos);
                totalDurationNanos = Math.addExact(totalDurationNanos, durationNanos);
            }
        }

        if (providerCallEvents == 0L) {
            minimumDurationNanos = 0L;
        }
        return new RecordingSummary(
                providerCallEvents,
                successEvents,
                minimumDurationNanos,
                maximumDurationNanos,
                totalDurationNanos
        );
    }

    public static void main(String[] args) throws Exception {
        Path recordingPath = args.length == 0
                ? Path.of("tmp", "notifyflow-threadpool-jfr-20260715.jfr")
                : Path.of(args[0]);
        ExperimentResult result = run(recordingPath);

        System.out.println("java_version=" + System.getProperty("java.version"));
        System.out.println("workers=" + result.execution().workers());
        System.out.println("queue_capacity=" + result.execution().queueCapacity());
        System.out.println("submitted=" + result.execution().submitted());
        System.out.println("accepted=" + result.execution().accepted());
        System.out.println("rejected=" + result.execution().rejected());
        System.out.println("completed=" + result.execution().completed());
        System.out.println("max_queue_depth=" + result.execution().maxQueueDepth());
        System.out.println("jfr_event_type=" + ProviderCallJfrEvent.EVENT_NAME);
        System.out.println("jfr_event_count=" + result.recording().providerCallEvents());
        System.out.println("jfr_success_events=" + result.recording().successEvents());
        System.out.println(
                "jfr_min_duration_nanos=" + result.recording().minimumDurationNanos()
        );
        System.out.println(
                "jfr_max_duration_nanos=" + result.recording().maximumDurationNanos()
        );
        System.out.println(
                "jfr_average_duration_nanos=" + result.recording().averageDurationNanos()
        );
        System.out.println("recording_path=" + recordingPath.toAbsolutePath());
        System.out.println("recording_bytes=" + result.recordingBytes());

        if (result.execution().submitted() != SUBMITTED_TASKS
                || result.execution().accepted() != 4L
                || result.execution().rejected() != 2L
                || result.execution().completed() != 4L
                || result.execution().maxQueueDepth() != QUEUE_CAPACITY
                || result.recording().providerCallEvents() != 4L
                || result.recording().successEvents() != 4L
                || result.recordingBytes() <= 0L) {
            throw new AssertionError("real ThreadPoolExecutor/JFR result is incomplete");
        }

        System.out.println("REAL_THREAD_POOL_JFR_EXPERIMENT_PASSED");
    }

    public record ExecutionSummary(
            long submitted,
            long accepted,
            long rejected,
            long completed,
            int maxQueueDepth,
            int workers,
            int queueCapacity
    ) {
    }

    public record RecordingSummary(
            long providerCallEvents,
            long successEvents,
            long minimumDurationNanos,
            long maximumDurationNanos,
            long totalDurationNanos
    ) {
        public long averageDurationNanos() {
            return providerCallEvents == 0L ? 0L : totalDurationNanos / providerCallEvents;
        }
    }

    public record ExperimentResult(
            ExecutionSummary execution,
            RecordingSummary recording,
            long recordingBytes
    ) {
    }
}
