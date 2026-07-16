package dev.notifyflow.course.notifyflow.application;

import java.time.Instant;

/** Time source port; production uses UTC and tests use a fixed clock. */
@FunctionalInterface
public interface Clock {
    Instant now();
}
