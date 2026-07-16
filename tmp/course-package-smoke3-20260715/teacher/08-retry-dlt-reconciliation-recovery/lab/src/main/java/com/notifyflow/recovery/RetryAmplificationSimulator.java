package com.notifyflow.recovery;

public final class RetryAmplificationSimulator {

    private RetryAmplificationSimulator() {
    }

    public static long countLeafCalls(int retryingLayers, int attemptsPerLayer) {
        if (retryingLayers < 1) {
            throw new IllegalArgumentException("retryingLayers must be at least 1");
        }
        if (attemptsPerLayer < 1) {
            throw new IllegalArgumentException("attemptsPerLayer must be at least 1");
        }
        return invokeLayer(retryingLayers, attemptsPerLayer);
    }

    private static long invokeLayer(int remainingLayers, int attemptsPerLayer) {
        if (remainingLayers == 0) {
            return 1L;
        }

        long leafCalls = 0L;
        for (int attempt = 0; attempt < attemptsPerLayer; attempt++) {
            leafCalls = Math.addExact(
                    leafCalls,
                    invokeLayer(remainingLayers - 1, attemptsPerLayer)
            );
        }
        return leafCalls;
    }
}

