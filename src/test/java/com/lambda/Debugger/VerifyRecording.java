package com.lambda.Debugger;

public final class VerifyRecording {

    private VerifyRecording() {
    }

    public static void main(String[] args) {
        try {
            verifyRecording(args);
            System.exit(0);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    static void verifyRecording(String[] args) {
        if (args.length < 2) {
            throw new IllegalArgumentException("Expected target class name and minimum timestamp count");
        }

        System.setProperty("DONT_INSTRUMENT", "true");
        Debugger.runLSD(new String[] { args[0] }, true);

        int minTimestamps = Integer.parseInt(args[1]);
        if (TimeStamp.nTSCreated < minTimestamps) {
            throw new IllegalStateException("Expected at least " + minTimestamps
                    + " timestamps for " + args[0] + " but recorded " + TimeStamp.nTSCreated);
        }

        for (int i = 2; i < args.length; i++) {
            String[] expected = args[i].split("#", 2);
            if (expected.length != 2) {
                throw new IllegalArgumentException("Expected trace as class#method: " + args[i]);
            }
            if (!hasTrace(expected[0], expected[1])) {
                throw new IllegalStateException("Missing trace for " + args[i]);
            }
        }
    }

    private static boolean hasTrace(String className, String methodName) {
        for (int i = 0; i < TraceLine.unfilteredTraceSets.length; i++) {
            VectorD traceSet = TraceLine.unfilteredTraceSets[i];
            if (traceSet == null) {
                continue;
            }
            for (int j = 0; j < traceSet.size(); j++) {
                Object line = traceSet.elementAt(j);
                if (line instanceof TraceLine) {
                    TraceLine traceLine = (TraceLine) line;
                    if (methodName.equals(traceLine.getMethod())
                            && className.equals(traceClassName(traceLine.thisObj))) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private static String traceClassName(Object receiver) {
        if (receiver instanceof Class) {
            return ((Class) receiver).getName();
        }
        if (receiver == null) {
            return null;
        }
        return receiver.getClass().getName();
    }
}
