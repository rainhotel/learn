package dev.notifyflow.course.notifyflow.application;

/** Read-only provider fact boundary used after an ambiguous delivery result. */
@FunctionalInterface
public interface ProviderQueryGateway {
    ProviderQueryResult query(ProviderQueryCommand command);
}
