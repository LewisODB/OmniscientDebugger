package com.lambda.Debugger;

public final class IntegrationInternalFailureHarness {
    private IntegrationInternalFailureHarness() {
    }

    public static void main(String[] args) throws Exception {
        IntegrationState.start(System.err, new String[] { "outside.Target" });
        for (int index = 0; index <= TimeStamp.MAX_THREADS; index++) {
            TimeStamp.getThreadIndex(new Thread("integration-thread-" + index));
        }
        System.exit(99);
    }
}
