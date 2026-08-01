package com.lambda.Debugger;

public final class IntegrationHistoryFailureHarness {
    private IntegrationHistoryFailureHarness() {
    }

    public static void main(String[] args) throws Exception {
        IntegrationState.start(System.err, new String[] { "outside.Target" });
        Debugger.programName = "outside.Target";
        if (args.length > 0 && "read".equals(args[0])) {
            DebuggerCommandHistoryList.readHistory();
        } else {
            DebuggerCommandHistoryList.writeHistory();
        }
        System.exit(99);
    }
}
