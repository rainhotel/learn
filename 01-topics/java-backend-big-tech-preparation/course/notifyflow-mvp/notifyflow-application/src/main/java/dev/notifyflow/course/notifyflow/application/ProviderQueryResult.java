package dev.notifyflow.course.notifyflow.application;

import java.util.Objects;

/** Provider-owned fact. PENDING/NOT_FOUND never prove delivery failure. */
public record ProviderQueryResult(
        Status status,
        String providerRequestId,
        String errorCategory,
        String errorCode) {

    public enum Status {
        SUCCEEDED,
        REJECTED,
        PENDING,
        NOT_FOUND
    }

    public ProviderQueryResult {
        status = Objects.requireNonNull(status, "status");
        if (status == Status.SUCCEEDED && (providerRequestId == null || providerRequestId.isBlank())) {
            throw new IllegalArgumentException("SUCCEEDED requires providerRequestId");
        }
        if (status == Status.REJECTED && (errorCategory == null || errorCategory.isBlank())) {
            throw new IllegalArgumentException("REJECTED requires errorCategory");
        }
    }

    public static ProviderQueryResult succeeded(String providerRequestId) {
        return new ProviderQueryResult(Status.SUCCEEDED, providerRequestId, null, null);
    }

    public static ProviderQueryResult rejected(
            String providerRequestId, String errorCategory, String errorCode) {
        return new ProviderQueryResult(Status.REJECTED, providerRequestId, errorCategory, errorCode);
    }

    public static ProviderQueryResult pending(String providerRequestId) {
        return new ProviderQueryResult(Status.PENDING, providerRequestId, null, null);
    }

    public static ProviderQueryResult notFound() {
        return new ProviderQueryResult(Status.NOT_FOUND, null, null, null);
    }
}
