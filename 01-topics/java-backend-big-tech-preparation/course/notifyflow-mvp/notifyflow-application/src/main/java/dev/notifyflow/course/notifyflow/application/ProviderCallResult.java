package dev.notifyflow.course.notifyflow.application;

import java.util.Objects;

/** Structured delivery fact; UNKNOWN means the side effect may already have happened. */
public record ProviderCallResult(
        Classification classification,
        String providerRequestId,
        String errorCategory,
        String errorCode) {

    public enum Classification {
        SUCCESS,
        REJECTED,
        UNKNOWN
    }

    public ProviderCallResult {
        classification = Objects.requireNonNull(classification, "classification");
        if (classification == Classification.SUCCESS && (providerRequestId == null || providerRequestId.isBlank())) {
            throw new IllegalArgumentException("SUCCESS requires providerRequestId");
        }
        if (classification != Classification.SUCCESS
                && (errorCategory == null || errorCategory.isBlank())) {
            throw new IllegalArgumentException("non-success result requires errorCategory");
        }
    }

    public static ProviderCallResult success(String providerRequestId) {
        return new ProviderCallResult(Classification.SUCCESS, providerRequestId, null, null);
    }

    public static ProviderCallResult rejected(
            String providerRequestId, String errorCategory, String errorCode) {
        return new ProviderCallResult(Classification.REJECTED, providerRequestId, errorCategory, errorCode);
    }

    public static ProviderCallResult unknown(
            String providerRequestId, String errorCategory, String errorCode) {
        return new ProviderCallResult(Classification.UNKNOWN, providerRequestId, errorCategory, errorCode);
    }
}
