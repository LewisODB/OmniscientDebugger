package com.lambda.Debugger;

import org.junit.Test;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;

@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class VerifyRecordingTest {

    @Test
    public void aDemoRecordsMain() {
        VerifyRecording.verifyRecording(new String[] {
            "com.lambda.Debugger.Demo",
            "1200",
            "com.lambda.Debugger.Demo#main",
        });
    }

    @Test
    public void bQuickSortRecordsSortNElements() {
        VerifyRecording.verifyRecording(new String[] {
            "com.lambda.Debugger.QuickSortNonThreaded",
            "300",
            "com.lambda.Debugger.QuickSortNonThreaded#sortNElements",
        });
    }

    @Test
    public void zArrayListMutationsReachShadowHistory() {
        VerifyRecording.verifyRecording(new String[] {
            "outside.ArrayListHistoryTarget",
            "5",
            "outside.ArrayListHistoryTarget#main",
            "@arraylist-history",
        });
    }
}
