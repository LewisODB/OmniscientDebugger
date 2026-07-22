package com.lambda.Debugger;

import java.io.PrintStream;

public final class IntegrationLauncher {
    private IntegrationLauncher() {
    }

    public static void main(String[] args) {
        run(args, true);
    }

    static void run(String[] args, boolean showController) {
        PrintStream originalError = System.err;
        try {
            IntegrationState.Launch launch = IntegrationState.start(originalError, args);
            Debugger.runIntegration(launch.target, launch.arguments, showController);
        } catch (IntegrationState.BadContract error) {
            throw IntegrationState.badContract(originalError, error.token, error.getMessage());
        } catch (Throwable error) {
            if (IntegrationState.isActive()) {
                throw IntegrationState.internalFailed("ODB integration failed.", error);
            }
            throw error;
        }
    }
}
