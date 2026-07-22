package com.lambda.Debugger;

public final class IntegrationManagedPathHarness {
    private IntegrationManagedPathHarness() {
    }

    public static void main(String[] args) throws Exception {
        IntegrationState.start(System.err, new String[] { "outside.Target" });
        System.out.println("defaults=" + IntegrationState.defaultsFile(".debuggerDefaults"));
        System.out.println("history=" + IntegrationState.commandHistoryFile("outside.Target"));
    }
}
