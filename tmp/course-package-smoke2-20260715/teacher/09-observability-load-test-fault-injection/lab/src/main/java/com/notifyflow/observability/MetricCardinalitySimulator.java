package com.notifyflow.observability;

import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

public final class MetricCardinalitySimulator {

    private static final Set<String> FORBIDDEN_TAG_KEYS = Set.of(
            "taskid",
            "userid",
            "traceid",
            "requestid",
            "eventid"
    );

    private MetricCardinalitySimulator() {
    }

    public static Result simulate(
            int eventCount,
            String[] providers,
            String[] results,
            boolean includeTaskId
    ) {
        if (eventCount <= 0) {
            throw new IllegalArgumentException("eventCount must be positive");
        }
        validateValues(providers, "providers");
        validateValues(results, "results");

        Set<String> series = new HashSet<>();
        for (int event = 0; event < eventCount; event++) {
            String provider = providers[event % providers.length];
            String result = results[(event / providers.length) % results.length];
            String key = "provider=" + provider + ",result=" + result;
            if (includeTaskId) {
                key += ",taskId=task-" + event;
            }
            series.add(key);
        }

        return new Result(eventCount, series.size(), includeTaskId);
    }

    public static boolean isForbiddenTagKey(String tagKey) {
        if (tagKey == null || tagKey.isBlank()) {
            throw new IllegalArgumentException("tagKey must not be blank");
        }
        String normalized = tagKey
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
        return FORBIDDEN_TAG_KEYS.contains(normalized);
    }

    private static void validateValues(String[] values, String label) {
        if (values == null || values.length == 0) {
            throw new IllegalArgumentException(label + " must not be empty");
        }
        for (String value : values) {
            if (value == null || value.isBlank()) {
                throw new IllegalArgumentException(label + " must not contain blank values");
            }
        }
    }

    public record Result(
            long eventCount,
            long uniqueSeries,
            boolean taskIdIncluded
    ) {
        public long estimatedMetadataBytes(long assumedBytesPerSeries) {
            if (assumedBytesPerSeries <= 0L) {
                throw new IllegalArgumentException("assumedBytesPerSeries must be positive");
            }
            return Math.multiplyExact(uniqueSeries, assumedBytesPerSeries);
        }
    }
}
