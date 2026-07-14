import java.time.Duration;
import java.util.List;
import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.BooleanSupplier;

public final class ThreadPoolLab {
    public static void main(String[] args) throws Exception {
        experimentPoolGrowthAndRejection();
        experimentCallerRunsBackpressure();
        experimentGracefulAndForcedShutdown();
        System.out.println("ALL_EXPERIMENTS_PASSED");
    }

    private static void experimentPoolGrowthAndRejection() throws Exception {
        System.out.println("\n=== Experiment 1: pool growth and rejection ===");
        CountDownLatch releaseWorkers = new CountDownLatch(1);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                4,
                30,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                namedFactory("growth"),
                new ThreadPoolExecutor.AbortPolicy());

        try {
            for (int id = 1; id <= 6; id++) {
                int taskId = id;
                executor.execute(() -> awaitRelease(taskId, releaseWorkers));
            }

            waitUntil(
                    () -> executor.getPoolSize() == 4
                            && executor.getActiveCount() == 4
                            && executor.getQueue().size() == 2,
                    Duration.ofSeconds(3));

            System.out.printf(
                    "before rejection: pool=%d active=%d queued=%d%n",
                    executor.getPoolSize(),
                    executor.getActiveCount(),
                    executor.getQueue().size());

            boolean rejected = false;
            try {
                executor.execute(() -> System.out.println("task 7 should not run"));
            } catch (RejectedExecutionException expected) {
                rejected = true;
                System.out.println("task 7 rejected by AbortPolicy");
            }

            check(rejected, "the seventh task should be rejected");
        } finally {
            releaseWorkers.countDown();
            orderlyShutdown(executor, Duration.ofSeconds(5));
        }
    }

    private static void experimentCallerRunsBackpressure() throws Exception {
        System.out.println("\n=== Experiment 2: CallerRunsPolicy backpressure ===");
        CountDownLatch releaseWorker = new CountDownLatch(1);
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                1,
                1,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(1),
                namedFactory("caller-runs"),
                new ThreadPoolExecutor.CallerRunsPolicy());

        try {
            executor.execute(() -> awaitRelease(1, releaseWorker));
            waitUntil(() -> executor.getActiveCount() == 1, Duration.ofSeconds(2));
            executor.execute(() -> System.out.println("queued task ran on " + Thread.currentThread().getName()));

            long started = System.nanoTime();
            String submittingThread = Thread.currentThread().getName();
            executor.execute(() -> {
                System.out.println("rejected task ran on " + Thread.currentThread().getName());
                sleep(Duration.ofMillis(250));
            });
            long elapsedMillis = TimeUnit.NANOSECONDS.toMillis(System.nanoTime() - started);

            System.out.printf("submitter=%s submission-blocked-for=%dms%n", submittingThread, elapsedMillis);
            check(elapsedMillis >= 200, "CallerRunsPolicy should slow the submitting thread");
        } finally {
            releaseWorker.countDown();
            orderlyShutdown(executor, Duration.ofSeconds(5));
        }
    }

    private static void experimentGracefulAndForcedShutdown() throws Exception {
        System.out.println("\n=== Experiment 3: shutdownNow and interruption ===");
        CountDownLatch workersStarted = new CountDownLatch(2);
        AtomicInteger interruptedTasks = new AtomicInteger();
        ThreadPoolExecutor executor = new ThreadPoolExecutor(
                2,
                2,
                0,
                TimeUnit.SECONDS,
                new ArrayBlockingQueue<>(2),
                namedFactory("shutdown"),
                new ThreadPoolExecutor.AbortPolicy());

        Runnable interruptibleTask = () -> {
            workersStarted.countDown();
            try {
                while (true) {
                    TimeUnit.SECONDS.sleep(10);
                }
            } catch (InterruptedException expected) {
                interruptedTasks.incrementAndGet();
                Thread.currentThread().interrupt();
                System.out.println("interruption observed by " + Thread.currentThread().getName());
            }
        };

        executor.execute(interruptibleTask);
        executor.execute(interruptibleTask);
        executor.execute(() -> System.out.println("queued task A should be returned"));
        executor.execute(() -> System.out.println("queued task B should be returned"));

        check(workersStarted.await(2, TimeUnit.SECONDS), "workers did not start in time");
        List<Runnable> neverStarted = executor.shutdownNow();
        check(executor.awaitTermination(3, TimeUnit.SECONDS), "executor did not terminate");

        System.out.printf(
                "never-started=%d interrupted-running-tasks=%d%n",
                neverStarted.size(),
                interruptedTasks.get());
        check(neverStarted.size() == 2, "two queued tasks should be returned by shutdownNow");
        check(interruptedTasks.get() == 2, "two running tasks should observe interruption");
    }

    private static void awaitRelease(int taskId, CountDownLatch latch) {
        System.out.printf("task %d started on %s%n", taskId, Thread.currentThread().getName());
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static ThreadFactory namedFactory(String prefix) {
        AtomicInteger sequence = new AtomicInteger();
        return task -> {
            Thread thread = new Thread(task);
            thread.setName(prefix + "-" + sequence.incrementAndGet());
            thread.setUncaughtExceptionHandler((t, error) ->
                    System.err.printf("uncaught error on %s: %s%n", t.getName(), error));
            return thread;
        };
    }

    private static void orderlyShutdown(ThreadPoolExecutor executor, Duration timeout)
            throws InterruptedException {
        executor.shutdown();
        if (!executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
            List<Runnable> neverStarted = executor.shutdownNow();
            System.err.println("forced shutdown; never-started tasks=" + neverStarted.size());
            if (!executor.awaitTermination(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new IllegalStateException("executor did not terminate");
            }
        }
    }

    private static void waitUntil(BooleanSupplier condition, Duration timeout) throws Exception {
        long deadline = System.nanoTime() + timeout.toNanos();
        while (!condition.getAsBoolean()) {
            if (System.nanoTime() >= deadline) {
                throw new AssertionError("condition was not met within " + timeout);
            }
            TimeUnit.MILLISECONDS.sleep(10);
        }
    }

    private static void sleep(Duration duration) {
        try {
            TimeUnit.MILLISECONDS.sleep(duration.toMillis());
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
