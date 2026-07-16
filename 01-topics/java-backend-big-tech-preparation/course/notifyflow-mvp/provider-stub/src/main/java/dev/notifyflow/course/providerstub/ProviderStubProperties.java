package dev.notifyflow.course.providerstub;

import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties("notifyflow.provider-stub")
public record ProviderStubProperties(Duration defaultCommitDelay, Duration maximumCommitDelay) {

    public ProviderStubProperties {
        defaultCommitDelay = defaultCommitDelay == null ? Duration.ofSeconds(2) : defaultCommitDelay;
        maximumCommitDelay = maximumCommitDelay == null ? Duration.ofSeconds(30) : maximumCommitDelay;
        if (defaultCommitDelay.isNegative() || defaultCommitDelay.isZero()) {
            throw new IllegalArgumentException("defaultCommitDelay must be positive");
        }
        if (maximumCommitDelay.isNegative() || maximumCommitDelay.isZero()) {
            throw new IllegalArgumentException("maximumCommitDelay must be positive");
        }
        if (defaultCommitDelay.compareTo(maximumCommitDelay) > 0) {
            throw new IllegalArgumentException("defaultCommitDelay must not exceed maximumCommitDelay");
        }
    }
}
