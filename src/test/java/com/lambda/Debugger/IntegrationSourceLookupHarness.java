package com.lambda.Debugger;

public final class IntegrationSourceLookupHarness {
    private IntegrationSourceLookupHarness() {
    }

    public static void main(String[] args) throws Exception {
        IntegrationState.start(System.err, new String[] { "outside.BehaviorTarget" });
        Defaults.readDefaults();
        IntegrationState.loadSourceDirectories();
        VectorD lines = CodePane.getDisplayList("BehaviorTarget.java", "outside.BehaviorTarget");
        if (lines.size() == 0) {
            throw new AssertionError("Integration source root did not load BehaviorTarget.java.");
        }
        System.out.println("source-lines=" + lines.size());
        System.out.println("default-dont-record=" + Defaults.dontRecord.size());
    }
}
