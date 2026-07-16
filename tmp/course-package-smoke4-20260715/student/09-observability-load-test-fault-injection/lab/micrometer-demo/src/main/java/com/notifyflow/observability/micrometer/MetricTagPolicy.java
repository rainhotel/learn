package com.notifyflow.observability.micrometer;

import io.micrometer.core.instrument.config.MeterFilter;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.Locale;
import java.util.Set;

@Configuration(proxyBeanMethods = false)
public class MetricTagPolicy {

    private static final Set<String> FORBIDDEN_KEYS = Set.of(
            "taskid",
            "userid",
            "traceid",
            "requestid",
            "eventid"
    );

    @Bean
    MeterFilter denyIdentityTags() {
        return MeterFilter.deny(id -> id.getTags().stream()
                .anyMatch(tag -> isForbidden(tag.getKey())));
    }

    static boolean isForbidden(String tagKey) {
        if (tagKey == null || tagKey.isBlank()) {
            throw new IllegalArgumentException("tagKey must not be blank");
        }
        String normalized = tagKey
                .toLowerCase(Locale.ROOT)
                .replace("_", "")
                .replace("-", "")
                .replace(" ", "");
        return FORBIDDEN_KEYS.contains(normalized);
    }
}
