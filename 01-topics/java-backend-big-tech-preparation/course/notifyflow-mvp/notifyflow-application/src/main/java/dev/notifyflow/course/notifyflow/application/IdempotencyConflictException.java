package dev.notifyflow.course.notifyflow.application;

public final class IdempotencyConflictException extends RuntimeException {
    public IdempotencyConflictException(String tenantId, String requestId) {
        super("Idempotency key was already used with a different request: " + tenantId + "/" + requestId);
    }
}
