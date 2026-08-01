package com.lambda.Debugger;

import java.io.ByteArrayOutputStream;
import java.io.PrintStream;

public final class IntegrationProtocolHarness {
    private IntegrationProtocolHarness() {
    }

    public static void main(String[] args) throws Exception {
        IntegrationState.start(System.err, new String[] { "outside.سلام" });
        System.setErr(new PrintStream(new ByteArrayOutputStream(), true, "UTF-8"));
        IntegrationState.targetLoaded("outside.سلام");
        IntegrationState.timestampAdded(3, 2);
        IntegrationState.timestampAdded(4, 3);
        IntegrationState.debuggerReady(4, 3);
    }
}
