package com.notifyflow.observability;

import java.nio.file.Files;
import java.nio.file.Path;

public final class RealThreadPoolJfrTest {

    public static void main(String[] args) throws Exception {
        Path recording = Path.of(
                "tmp",
                "observability-real-threadpool-test-20260715.jfr"
        );
        ThreadPoolJfrExperiment.ExperimentResult result =
                ThreadPoolJfrExperiment.run(recording);

        assertEquals(6L, result.execution().submitted(), "submitted tasks");
        assertEquals(4L, result.execution().accepted(), "accepted tasks");
        assertEquals(2L, result.execution().rejected(), "rejected tasks");
        assertEquals(4L, result.execution().completed(), "completed tasks");
        assertEquals(2L, result.execution().maxQueueDepth(), "max queue depth");
        assertEquals(4L, result.recording().providerCallEvents(), "JFR event count");
        assertEquals(4L, result.recording().successEvents(), "successful JFR events");
        assertTrue(result.recording().minimumDurationNanos() >= 0L, "minimum duration");
        assertTrue(
                result.recording().maximumDurationNanos()
                        >= result.recording().minimumDurationNanos(),
                "duration range"
        );
        assertTrue(Files.isRegularFile(recording), "recording file must exist");
        assertTrue(Files.size(recording) > 0L, "recording file must not be empty");

        System.out.println("ALL_REAL_THREAD_POOL_JFR_TESTS_PASSED");
    }

    private static void assertEquals(long expected, long actual, String message) {
        if (expected != actual) {
            throw new AssertionError(message + ": expected=" + expected + ", actual=" + actual);
        }
    }

    private static void assertTrue(boolean condition, String message) {
        if (!condition) {
            throw new AssertionError(message);
        }
    }
}
