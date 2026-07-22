package com.lambda.Debugger;

public final class IntegrationStrictLoaderHarness {
    private IntegrationStrictLoaderHarness() {
    }

    public static void main(String[] args) throws Exception {
        final DebugifyingClassLoader loader = new DebugifyingClassLoader() {
            protected Class findClass(String className, boolean instrument) {
                throw new VerifyError("fixture instrumentation failure");
            }
        };
        IntegrationState.start(System.err, new String[] { "outside.StrictFallbackTarget" });
        if ("lazy".equals(args[0])) {
            Thread thread = new Thread(new Runnable() {
                public void run() {
                    try {
                        loader.loadClass("outside.StrictFallbackTarget");
                    } catch (ClassNotFoundException error) {
                        throw new AssertionError(error);
                    }
                }
            }, "lazy-instrumentation-fixture");
            thread.start();
            thread.join();
        } else {
            loader.loadClass("outside.StrictFallbackTarget");
        }
        System.exit(99);
    }
}
