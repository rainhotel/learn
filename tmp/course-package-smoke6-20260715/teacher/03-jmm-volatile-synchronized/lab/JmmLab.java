import java.util.concurrent.CountDownLatch;
import java.util.concurrent.CyclicBarrier;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public final class JmmLab {
    public static void main(String[] args) throws Exception {
        experimentVolatilePublication();
        experimentVolatileIsNotAtomic();
        experimentSynchronizedIncrement();
        experimentMonitorHappensBefore();
        experimentReentrancyAndAbruptUnlock();
        System.out.println("ALL_EXPERIMENTS_PASSED");
    }

    private static void experimentVolatilePublication() throws Exception {
        System.out.println("\n=== Experiment 1: volatile publication ===");
        Publication publication = new Publication();
        CountDownLatch readerStarted = new CountDownLatch(1);
        AtomicInteger observed = new AtomicInteger(-1);

        Thread reader = Thread.ofPlatform().name("volatile-reader").start(() -> {
            readerStarted.countDown();
            while (!publication.ready) {
                Thread.onSpinWait();
            }
            observed.set(publication.payload);
        });

        check(readerStarted.await(2, TimeUnit.SECONDS), "reader did not start");
        publication.payload = 42;
        publication.ready = true;
        reader.join();

        System.out.println("observed payload=" + observed.get());
        check(observed.get() == 42, "volatile publication should expose prior payload write");
    }

    private static void experimentVolatileIsNotAtomic() throws Exception {
        System.out.println("\n=== Experiment 2: volatile increment is not atomic ===");
        int rounds = 5_000;
        VolatileCounter counter = new VolatileCounter();
        CyclicBarrier afterRead = new CyclicBarrier(2);
        CyclicBarrier afterWrite = new CyclicBarrier(2);

        Runnable increment = () -> {
            try {
                for (int i = 0; i < rounds; i++) {
                    int local = counter.value;
                    afterRead.await();
                    counter.value = local + 1;
                    afterWrite.await();
                }
            } catch (Exception e) {
                throw new RuntimeException(e);
            }
        };

        Thread first = Thread.ofPlatform().name("volatile-increment-1").start(increment);
        Thread second = Thread.ofPlatform().name("volatile-increment-2").start(increment);
        first.join();
        second.join();

        System.out.printf("expected-if-atomic=%d actual=%d%n", rounds * 2, counter.value);
        check(counter.value == rounds, "coordinated read-modify-write should lose one update per round");
    }

    private static void experimentSynchronizedIncrement() throws Exception {
        System.out.println("\n=== Experiment 3: synchronized compound action ===");
        int incrementsPerThread = 100_000;
        SynchronizedCounter counter = new SynchronizedCounter();

        Runnable increment = () -> {
            for (int i = 0; i < incrementsPerThread; i++) {
                counter.increment();
            }
        };

        Thread first = Thread.ofPlatform().name("sync-increment-1").start(increment);
        Thread second = Thread.ofPlatform().name("sync-increment-2").start(increment);
        first.join();
        second.join();

        int expected = incrementsPerThread * 2;
        System.out.printf("expected=%d actual=%d%n", expected, counter.get());
        check(counter.get() == expected, "synchronized increment should preserve all updates");
    }

    private static void experimentMonitorHappensBefore() throws Exception {
        System.out.println("\n=== Experiment 4: monitor unlock -> subsequent lock ===");
        Object monitor = new Object();
        PlainState state = new PlainState();
        CountDownLatch readerReady = new CountDownLatch(1);
        AtomicInteger observed = new AtomicInteger(-1);

        Thread reader;
        synchronized (monitor) {
            reader = Thread.ofPlatform().name("monitor-reader").start(() -> {
                readerReady.countDown();
                synchronized (monitor) {
                    observed.set(state.payload);
                }
            });

            check(readerReady.await(2, TimeUnit.SECONDS), "reader did not attempt monitor acquisition");
            state.payload = 99;
        }

        reader.join();
        System.out.println("observed payload=" + observed.get());
        check(observed.get() == 99, "reader should observe write preceding monitor unlock");
    }

    private static void experimentReentrancyAndAbruptUnlock() throws Exception {
        System.out.println("\n=== Experiment 5: reentrancy and abrupt completion ===");
        ReentrantService service = new ReentrantService();
        check(service.outer() == 2, "the same thread should reenter the same monitor");

        Object monitor = new Object();
        CountDownLatch entered = new CountDownLatch(1);
        Thread failing = Thread.ofPlatform()
                .name("failing-owner")
                .uncaughtExceptionHandler((thread, error) ->
                        System.out.println(thread.getName() + " ended with " + error.getClass().getSimpleName()))
                .start(() -> {
                    synchronized (monitor) {
                        entered.countDown();
                        throw new IllegalStateException("simulated failure");
                    }
                });

        check(entered.await(2, TimeUnit.SECONDS), "failing thread did not enter monitor");
        failing.join();

        AtomicInteger acquiredAfterFailure = new AtomicInteger();
        Thread follower = Thread.ofPlatform().name("follower").start(() -> {
            synchronized (monitor) {
                acquiredAfterFailure.incrementAndGet();
            }
        });
        follower.join();

        System.out.println("follower acquisitions=" + acquiredAfterFailure.get());
        check(acquiredAfterFailure.get() == 1, "monitor should be released after abrupt completion");
    }

    private static void check(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }

    private static final class Publication {
        private int payload;
        private volatile boolean ready;
    }

    private static final class VolatileCounter {
        private volatile int value;
    }

    private static final class SynchronizedCounter {
        private int value;

        synchronized void increment() {
            value++;
        }

        synchronized int get() {
            return value;
        }
    }

    private static final class PlainState {
        private int payload;
    }

    private static final class ReentrantService {
        synchronized int outer() {
            return 1 + inner();
        }

        synchronized int inner() {
            return 1;
        }
    }
}
