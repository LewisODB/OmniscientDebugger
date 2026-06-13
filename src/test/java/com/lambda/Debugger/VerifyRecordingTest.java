package com.lambda.Debugger;

import org.junit.Test;

public class VerifyRecordingTest {

    @Test
    public void demoRecordsMain() {
        VerifyRecording.verifyRecording(new String[] {
            "com.lambda.Debugger.Demo",
            "1200",
            "com.lambda.Debugger.Demo#main",
        });
    }

    @Test
    public void quickSortRecordsSortNElements() {
        VerifyRecording.verifyRecording(new String[] {
            "com.lambda.Debugger.QuickSortNonThreaded",
            "300",
            "com.lambda.Debugger.QuickSortNonThreaded#sortNElements",
        });
    }
}
