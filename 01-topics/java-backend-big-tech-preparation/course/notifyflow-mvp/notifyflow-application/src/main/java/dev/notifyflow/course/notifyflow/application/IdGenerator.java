package dev.notifyflow.course.notifyflow.application;

/** Identifier source port for event ids (and future case/attempt ids). */
@FunctionalInterface
public interface IdGenerator {
    String nextId();
}
