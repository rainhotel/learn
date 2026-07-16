package com.notifyflow.recovery;

public final class RetryAmplificationExperiment {

    private RetryAmplificationExperiment() {
    }

    public static void main(String[] args) {
        int layers = 5;
        int attemptsPerLayer = 3;

        long layeredCalls = RetryAmplificationSimulator.countLeafCalls(
                layers,
                attemptsPerLayer
        );
        long singleOwnerCalls = RetryAmplificationSimulator.countLeafCalls(
                1,
                attemptsPerLayer
        );
        long amplification = layeredCalls / singleOwnerCalls;

        System.out.println("layers=" + layers);
        System.out.println("attempts_per_layer=" + attemptsPerLayer);
        System.out.println("layered_leaf_calls=" + layeredCalls);
        System.out.println("single_owner_leaf_calls=" + singleOwnerCalls);
        System.out.println("load_amplification_vs_single_owner=" + amplification + "x");

        if (layeredCalls != 243L || singleOwnerCalls != 3L) {
            throw new AssertionError("retry amplification result is unexpected");
        }

        System.out.println("RETRY_AMPLIFICATION_EXPERIMENT_PASSED");
    }
}

