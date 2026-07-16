package dev.notifyflow.course.notifyflow.domain;

import java.time.Instant;
import java.util.Objects;

/** Immutable record of provider fact reconciliation for one UNKNOWN attempt. */
public record ReconciliationCase(
        String caseId,
        long taskId,
        String attemptId,
        ReconciliationStatus status,
        int queryCount,
        Instant nextQueryAt,
        Instant deadlineAt,
        String lastProviderStatus,
        long version,
        Instant createdAt,
        Instant updatedAt,
        Instant resolvedAt) {

    public ReconciliationCase {
        caseId = required(caseId, "caseId");
        if (taskId <= 0) {
            throw new IllegalArgumentException("taskId must be positive");
        }
        attemptId = required(attemptId, "attemptId");
        status = Objects.requireNonNull(status, "status");
        if (queryCount < 0 || version < 0) {
            throw new IllegalArgumentException("queryCount/version must not be negative");
        }
        nextQueryAt = Objects.requireNonNull(nextQueryAt, "nextQueryAt");
        deadlineAt = Objects.requireNonNull(deadlineAt, "deadlineAt");
        createdAt = Objects.requireNonNull(createdAt, "createdAt");
        updatedAt = Objects.requireNonNull(updatedAt, "updatedAt");
    }

    public ReconciliationCase beginQuery(Instant at) {
        Objects.requireNonNull(at, "at");
        status.transitionTo(ReconciliationStatus.QUERYING);
        return copy(ReconciliationStatus.QUERYING, queryCount + 1, nextQueryAt, lastProviderStatus,
                version + 1, at, null);
    }

    public ReconciliationCase reopen(Instant next, String providerStatus, Instant at) {
        Objects.requireNonNull(next, "next");
        Objects.requireNonNull(at, "at");
        status.transitionTo(ReconciliationStatus.OPEN);
        return copy(ReconciliationStatus.OPEN, queryCount, next, providerStatus, version + 1, at, null);
    }

    public ReconciliationCase resolve(String providerStatus, Instant at) {
        Objects.requireNonNull(at, "at");
        status.transitionTo(ReconciliationStatus.RESOLVED);
        return copy(ReconciliationStatus.RESOLVED, queryCount, nextQueryAt, providerStatus, version + 1, at, at);
    }

    public ReconciliationCase moveToManualReview(String providerStatus, Instant at) {
        Objects.requireNonNull(at, "at");
        status.transitionTo(ReconciliationStatus.MANUAL_REVIEW);
        return copy(ReconciliationStatus.MANUAL_REVIEW, queryCount, nextQueryAt, providerStatus, version + 1, at, at);
    }

    private ReconciliationCase copy(ReconciliationStatus nextStatus, int nextCount, Instant nextQuery,
                                    String providerStatus, long nextVersion, Instant at, Instant resolved) {
        return new ReconciliationCase(caseId, taskId, attemptId, nextStatus, nextCount, nextQuery,
                deadlineAt, providerStatus, nextVersion, createdAt, at, resolved);
    }

    private static String required(String value, String name) {
        Objects.requireNonNull(value, name);
        if (value.isBlank()) {
            throw new IllegalArgumentException(name + " must not be blank");
        }
        return value;
    }
}
