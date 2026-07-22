package com.lambda.Debugger;

public final class IntegrationHeadlessLauncherHarness {
    private IntegrationHeadlessLauncherHarness() {
    }

    public static void main(String[] args) {
        IntegrationLauncher.run(args, false);
    }
}
