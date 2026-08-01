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

        verifyTraceCollectionViews();

        for (int i = 2; i < args.length; i++) {
            if ("@arraylist-history".equals(args[i])) {
                if (!hasArrayListMutationHistory()) {
                    throw new IllegalStateException(
                            "Missing MyArrayList mutation history");
                }
                verifyShadowIdentityTableAndTimelineSwap();
                continue;
            }
            String[] expected = args[i].split("#", 2);
            if (expected.length != 2) {
                throw new IllegalArgumentException("Expected trace as class#method: " + args[i]);
            }
            if (!hasTrace(expected[0], expected[1])) {
                throw new IllegalStateException("Missing trace for " + args[i]);
            }
        }
    }

    private static boolean hasArrayListMutationHistory() {
        java.util.Iterator shadows = Shadow.getIterator();
        while (shadows.hasNext()) {
            Shadow shadow = (Shadow) shadows.next();
            if (!(shadow.obj instanceof MyArrayList)) {
                continue;
            }
            java.util.Set times = new java.util.HashSet();
            boolean sawFirst = false;
            boolean sawZeroth = false;
            boolean sawChanged = false;
            boolean cleared = true;
            for (int i = 0; i < shadow.size(); i++) {
                HistoryList history = shadow.getShadowVar(i);
                if (history == null) {
                    continue;
                }
                for (int j = 0; j < history.size(); j++) {
                    Object value = history.getValue(j);
                    sawFirst |= "first".equals(value);
                    sawZeroth |= "zeroth".equals(value);
                    sawChanged |= "changed".equals(value);
                    times.add(Integer.valueOf(history.getTime(j)));
                }
                cleared &= history.getLastValue() == Dashes.DASHES;
            }
            if (sawFirst && sawZeroth && sawChanged && cleared
                    && times.size() >= 5) {
                return true;
            }
        }
        return false;
    }

    private static void verifyTraceCollectionViews() {
        for (int i = 0; i < TraceLine.unfilteredTraceSets.length; i++) {
            VectorD traceSet = TraceLine.unfilteredTraceSets[i];
            if (traceSet == null) {
                continue;
            }
            java.util.Iterator iterator = traceSet.iterator();
            java.util.Enumeration enumeration = traceSet.elements();
            for (int j = 0; j < traceSet.size(); j++) {
                Object indexed = traceSet.elementAt(j);
                if (!iterator.hasNext() || iterator.next() != indexed) {
                    throw new IllegalStateException(
                            "Trace iterator differs from indexed storage");
                }
                if (!enumeration.hasMoreElements()
                        || enumeration.nextElement() != indexed) {
                    throw new IllegalStateException(
                            "Trace enumeration differs from indexed storage");
                }
            }
            if (iterator.hasNext() || enumeration.hasMoreElements()) {
                throw new IllegalStateException(
                        "Trace collection view has extra entries");
            }
        }
    }

    private static void verifyShadowIdentityTableAndTimelineSwap() {
        HashMapEq original = Shadow.getTable();
        if (original.isEmpty()) {
            throw new IllegalStateException("Shadow identity table is empty");
        }
        java.util.Map.Entry sample = (java.util.Map.Entry)
                original.entrySet().iterator().next();
        if (original.get(sample.getKey()) != sample.getValue()) {
            throw new IllegalStateException("Shadow identity lookup changed");
        }

        EqualObject firstEqual = new EqualObject();
        EqualObject secondEqual = new EqualObject();
        Shadow firstEqualShadow = Shadow.get(firstEqual);
        Shadow secondEqualShadow = Shadow.get(secondEqual);
        if (firstEqualShadow == secondEqualShadow
                || original.get(firstEqual) != firstEqualShadow
                || original.get(secondEqual) != secondEqualShadow) {
            throw new IllegalStateException(
                    "Shadow collapsed equal-but-distinct identity keys");
        }
        int equalKeysSeen = 0;
        java.util.Iterator keys = original.keySet().iterator();
        while (keys.hasNext()) {
            Object key = keys.next();
            if (key == firstEqual || key == secondEqual) {
                equalKeysSeen++;
            }
        }
        if (equalKeysSeen != 2) {
            throw new IllegalStateException(
                    "Shadow identity keys did not survive table iteration");
        }

        Thread thread = Thread.currentThread();
        Object lock = new Object();
        Shadow threadShadow = Shadow.get(thread);
        Shadow lockShadow = Shadow.get(lock);
        int time = TimeStamp.nTSCreated;
        threadShadow.threadGetting(time, lock, null);
        lockShadow.addSleeper(time, thread, null);
        if (Shadow.getBlockedHL(thread) == null
                || lockShadow.getSleeperSet() == null) {
            throw new IllegalStateException(
                    "Shadow blocked/sleeper identity tables lost entries");
        }

        Shadow.switchTimeLines(false);
        HashMapEq alternate = Shadow.getTable();
        Shadow alternateThread = (Shadow) alternate.get(thread);
        Shadow alternateLock = (Shadow) alternate.get(lock);
        if (alternate == original || alternate.get(sample.getKey()) == null
                || alternate.get(firstEqual) == null
                || alternate.get(secondEqual) == null
                || alternate.get(firstEqual) == alternate.get(secondEqual)
                || alternateThread == null
                || alternateThread.getBlockedHL() == null
                || alternateLock == null
                || alternateLock.getSleeperSet() == null) {
            throw new IllegalStateException("Shadow timeline table did not swap");
        }
        Shadow.switchTimeLines(false);
    }

    private static final class EqualObject {
        public boolean equals(Object other) {
            return other instanceof EqualObject;
        }

        public int hashCode() {
            return 1;
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
