package dev.notifyflow.course.notifyflow.application;

/** External side-effect boundary. The adapter owns transport error classification. */
@FunctionalInterface
public interface ProviderGateway {
    ProviderCallResult deliver(ProviderDeliveryCommand command);
}
