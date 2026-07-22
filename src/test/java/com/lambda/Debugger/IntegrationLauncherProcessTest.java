package com.lambda.Debugger;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.concurrent.TimeUnit;

import org.junit.Test;

public class IntegrationLauncherProcessTest {
    private static final String TOKEN = "0123456789abcdef0123456789abcdef";

    @Test
    public void missingTargetFailsTheContractBeforeOdbInitialization() throws Exception {
        Path state = Files.createTempDirectory("odb-integration-state");

        Result result = launchMain(state, TOKEN, "com.lambda.Debugger.IntegrationLauncher");

        assertEquals(2, result.exitCode);
        assertTrue(result.stderr, result.stderr.contains("@@ODB-INTEGRATION@@\t" + TOKEN + "\t"));
        assertTrue(result.stderr, result.stderr.contains("\"type\":\"fatal\""));
        assertTrue(result.stderr, result.stderr.contains("\"code\":\"BAD_CONTRACT\""));
        assertFalse(result.stderr, result.stderr.contains("Lewis Omniscient Debugger"));
    }

    @Test
    public void missingTargetClassFailsAfterCreatingOnlyManagedDefaults() throws Exception {
        Path state = Files.createTempDirectory("odb-integration-state");

        Result result = launch(state, TOKEN, "outside.MissingTarget");

        assertEquals(1, result.exitCode);
        assertTrue(result.stderr, result.stderr.contains("\"type\":\"runtime-ready\""));
        assertTrue(result.stderr, result.stderr.contains("\"code\":\"TARGET_CLASS_NOT_FOUND\""));
        assertTrue(Files.isRegularFile(state.resolve(".debuggerDefaults")));
        assertFalse(Files.exists(result.workingDirectory.resolve(".debuggerDefaults")));
    }

    @Test
    public void targetWithoutPublicStaticVoidMainFailsExplicitly() throws Exception {
        Path state = Files.createTempDirectory("odb-integration-state");

        Result result = launch(state, TOKEN, "outside.InvalidMainTarget");

        assertEquals(1, result.exitCode);
        assertTrue(result.stderr, result.stderr.contains("\"code\":\"MAIN_METHOD_INVALID\""));
        assertFalse(result.stderr, result.stderr.contains("\"type\":\"target-loaded\""));
    }

    @Test
    public void targetThatProducesNoRecordingFailsBeforeOpeningDebugger() throws Exception {
        Path state = Files.createTempDirectory("odb-integration-state");

        Result result = launch(state, TOKEN, "outside.NoRecordingTarget");

        assertEquals(1, result.exitCode);
        assertTrue(result.stderr, result.stderr.contains("\"type\":\"target-loaded\""));
        assertTrue(result.stderr, result.stderr.contains("\"code\":\"NO_RECORDING\""));
        assertFalse(result.stderr, result.stderr.contains("\"type\":\"debugger-ready\""));
    }

    @Test
    public void externalTargetKeepsLaunchInputsAndEventsUseOriginalStderr() throws Exception {
        Path state = Files.createTempDirectory("odb-integration-state");

        Result result = launch(state, TOKEN, "outside.BehaviorTarget", "one", "two words", "السلام");

        assertEquals(23, result.exitCode);
        assertTrue(result.stdout, result.stdout.contains("args=[one, two words, السلام]"));
        assertTrue(result.stdout, result.stdout.contains("user.dir=" + result.workingDirectory.toRealPath()));
        assertTrue(result.stdout, result.stdout.contains("cwd=" + result.workingDirectory.toRealPath()));
        assertTrue(result.stdout, result.stdout.contains("classpath-helper=kept"));
        assertTrue(result.stdout, result.stdout.contains("state-property=null"));
        assertTrue(result.stdout, result.stdout.contains("token-property=null"));
        assertTrue(Files.isRegularFile(result.workingDirectory.resolve("target-relative.txt")));
        assertTrue(Files.isRegularFile(result.workingDirectory.resolve("redirected-stderr.txt")));
        assertTrue(result.stderr, result.stderr.contains("\"type\":\"runtime-ready\""));
        assertTrue(result.stderr, result.stderr.contains("\"type\":\"target-loaded\""));
        assertTrue(result.stderr, result.stderr.contains("\"type\":\"recording-started\""));
        assertTrue(Files.isRegularFile(state.resolve(".debuggerDefaults")));
    }

    @Test
    public void eagerAndLazyInstrumentationFailuresAreFatal() throws Exception {
        for (String mode : Arrays.asList("eager", "lazy")) {
            Path state = Files.createTempDirectory("odb-integration-state");

            Result result = launchMain(
                    state,
                    TOKEN,
                    "com.lambda.Debugger.IntegrationStrictLoaderHarness",
                    mode);

            assertEquals(mode + "\n" + result.stderr, 1, result.exitCode);
            assertTrue(result.stderr, result.stderr.contains("\"code\":\"INSTRUMENTATION_FAILED\""));
            assertTrue(result.stderr, result.stderr.contains("\"class\":\"outside.StrictFallbackTarget\""));
        }
    }

    @Test
    public void invalidStateTokenTargetAndForbiddenFlagsFailTheContract() throws Exception {
        Path state = Files.createTempDirectory("odb-integration-state");
        Path stateFile = Files.createTempFile("odb-integration-state-file", ".tmp");
        Path missingState = state.resolve("missing-state");
        Path unwritableState = Files.createTempDirectory("odb-integration-unwritable");
        assertTrue("could not make state fixture non-writable", unwritableState.toFile().setWritable(false, false));
        assertFalse("state fixture remained writable", Files.isWritable(unwritableState));
        Result unwritableResult;
        try {
            unwritableResult = launchRaw(
                    unwritableState.toString(),
                    TOKEN,
                    new String[0],
                    "outside.MissingTarget");
        } finally {
            assertTrue("could not restore state fixture", unwritableState.toFile().setWritable(true, false));
        }
        List<Result> results = new ArrayList<Result>(Arrays.asList(
                launchRaw(null, TOKEN, new String[0], "outside.MissingTarget"),
                launchRaw("relative-state", TOKEN, new String[0], "outside.MissingTarget"),
                launchRaw(missingState.toString(), TOKEN, new String[0], "outside.MissingTarget"),
                launchRaw(stateFile.toString(), TOKEN, new String[0], "outside.MissingTarget"),
                unwritableResult,
                launchRaw(state.toString(), "ABCDEF", new String[0], "outside.MissingTarget"),
                launchRaw(state.toString(), "bad\nسلام", new String[0], "outside.MissingTarget"),
                launchRaw(state.toString(), TOKEN, new String[0], "../Target")));
        for (String flag : Arrays.asList(
                "DONT_INSTRUMENT",
                "DONT_START",
                "PAUSED",
                "DONT_SHOW",
                "NO_WINDOWS",
                "NO_DEFAULTS",
                "DEBUGIFY_ONLY",
                "DONT_KILL_TARGET")) {
            results.add(launchRaw(
                    state.toString(),
                    TOKEN,
                    new String[] { "-D" + flag + "=true" },
                    "outside.MissingTarget"));
        }

        for (Result result : results) {
            assertEquals(result.stderr, 2, result.exitCode);
            assertTrue(result.stderr, result.stderr.contains("\"code\":\"BAD_CONTRACT\""));
            assertFalse(result.stderr, result.stderr.contains("\"type\":\"runtime-ready\""));
        }
        Result unsafeToken = results.get(6);
        assertFalse(unsafeToken.stderr, unsafeToken.stderr.contains("سلام"));
        assertEquals(1, occurrences(unsafeToken.stderr, "\n"));
    }

    @Test
    public void defaultsAndCommandHistoryResolveUnderManagedState() throws Exception {
        Path state = Files.createTempDirectory("odb-integration-state");

        Result result = launchMain(
                state,
                TOKEN,
                "com.lambda.Debugger.IntegrationManagedPathHarness");

        assertEquals(result.stderr, 0, result.exitCode);
        assertTrue(result.stdout, result.stdout.contains("defaults=" + state.toRealPath().resolve(".debuggerDefaults")));
        assertTrue(
                result.stdout,
                result.stdout.contains("history=" + state.toRealPath().resolve("outside.Target.debuggerCommands")));
        assertFalse(Files.exists(result.workingDirectory.resolve(".debuggerDefaults")));
        assertFalse(Files.exists(result.workingDirectory.resolve("outside.Target.debuggerCommands")));
    }

    @Test
    public void defaultsIoAndInternalInitializationFailuresAreExplicit() throws Exception {
        Path badDefaultsState = Files.createTempDirectory("odb-integration-state");
        Files.createDirectory(badDefaultsState.resolve(".debuggerDefaults"));

        Result defaultsFailure = launch(badDefaultsState, TOKEN, "outside.MissingTarget");
        Result internalFailure = launchRaw(
                Files.createTempDirectory("odb-integration-state").toString(),
                TOKEN,
                new String[] { "-DMEMORY=not-a-number" },
                "outside.MissingTarget");

        assertEquals(defaultsFailure.stderr, 1, defaultsFailure.exitCode);
        assertTrue(defaultsFailure.stderr, defaultsFailure.stderr.contains("\"code\":\"DEFAULTS_IO\""));
        assertEquals(internalFailure.stderr, 1, internalFailure.exitCode);
        assertTrue(internalFailure.stderr, internalFailure.stderr.contains("\"code\":\"INTERNAL_ERROR\""));
    }

    @Test
    public void legacyMainKeepsHumanFailureAndWorkingDirectoryDefaults() throws Exception {
        Result result = launchProcess(
                null,
                null,
                new String[0],
                "com.lambda.Debugger.Debugger",
                "outside.LegacyMissingTarget");

        assertEquals(1, result.exitCode);
        assertTrue(result.stderr, result.stderr.contains("Class not found: outside.LegacyMissingTarget"));
        assertFalse(result.stderr, result.stderr.contains("@@ODB-INTEGRATION@@"));
        assertTrue(Files.isRegularFile(result.workingDirectory.resolve(".debuggerDefaults")));
    }

    @Test
    public void protocolIsAsciiOrderedAndUsesCapturedOriginalStderr() throws Exception {
        Path state = Files.createTempDirectory("odb-integration-state");

        Result result = launchMain(
                state,
                TOKEN,
                "com.lambda.Debugger.IntegrationProtocolHarness");

        assertEquals(result.stderr, 0, result.exitCode);
        assertFalse(result.stderr, result.stderr.contains("سلام"));
        assertTrue(result.stderr, result.stderr.contains("\\u0633\\u0644\\u0627\\u0645"));
        assertOrdered(result.stderr, "\"sequence\":1", "\"sequence\":2", "\"sequence\":3", "\"sequence\":4");
        assertEquals(1, occurrences(result.stderr, "\"type\":\"recording-started\""));
        assertTrue(result.stderr, result.stderr.contains("\"type\":\"debugger-ready\""));
        for (int index = 0; index < result.stderr.length(); index++) {
            assertTrue(result.stderr, result.stderr.charAt(index) <= 0x7f);
        }
    }

    @Test
    public void primaryTargetExcludedFromInstrumentationNeverRunsUninstrumented() throws Exception {
        Path state = Files.createTempDirectory("odb-integration-state");
        Files.write(
                state.resolve(".debuggerDefaults"),
                "OnlyInstrument: \"some.other.package.\"\n".getBytes(StandardCharsets.UTF_8));

        Result result = launch(state, TOKEN, "outside.BehaviorTarget", "must-not-run");

        assertEquals(result.stderr, 1, result.exitCode);
        assertTrue(result.stderr, result.stderr.contains("\"code\":\"INSTRUMENTATION_FAILED\""));
        assertFalse(result.stderr, result.stderr.contains("\"type\":\"target-loaded\""));
        assertFalse(Files.exists(result.workingDirectory.resolve("target-relative.txt")));
    }

    @Test
    public void commandHistoryIoFailureIsFatalInIntegrationMode() throws Exception {
        Path writeState = Files.createTempDirectory("odb-integration-state");
        Files.createDirectory(writeState.resolve("outside.Target.debuggerCommands"));
        Path readState = Files.createTempDirectory("odb-integration-state");
        Files.createDirectory(readState.resolve("outside.Target.debuggerCommands"));

        Result writeResult = launchMain(
                writeState,
                TOKEN,
                "com.lambda.Debugger.IntegrationHistoryFailureHarness");
        Result readResult = launchMain(
                readState,
                TOKEN,
                "com.lambda.Debugger.IntegrationHistoryFailureHarness",
                "read");

        for (Result result : Arrays.asList(writeResult, readResult)) {
            assertEquals(result.stderr, 1, result.exitCode);
            assertTrue(result.stderr, result.stderr.contains("\"code\":\"DEFAULTS_IO\""));
        }
    }

    @Test
    public void managedWritesDoNotFollowSymlinksOutsideState() throws Exception {
        Path outside = Files.createTempFile("odb-integration-outside", ".txt");
        Files.write(outside, "unchanged".getBytes(StandardCharsets.UTF_8));
        Path defaultsState = Files.createTempDirectory("odb-integration-state");
        Files.createSymbolicLink(defaultsState.resolve(".debuggerDefaults"), outside);
        Path historyState = Files.createTempDirectory("odb-integration-state");
        Files.createSymbolicLink(historyState.resolve("outside.Target.debuggerCommands"), outside);

        Result defaultsResult = launch(defaultsState, TOKEN, "outside.MissingTarget");
        Result historyResult = launchMain(
                historyState,
                TOKEN,
                "com.lambda.Debugger.IntegrationHistoryFailureHarness");

        for (Result result : Arrays.asList(defaultsResult, historyResult)) {
            assertEquals(result.stderr, 1, result.exitCode);
            assertTrue(result.stderr, result.stderr.contains("\"code\":\"DEFAULTS_IO\""));
        }
        assertEquals("unchanged", new String(Files.readAllBytes(outside), StandardCharsets.UTF_8));
    }

    @Test
    public void directInternalFailureEmitsFatalBeforeExit() throws Exception {
        Path state = Files.createTempDirectory("odb-integration-state");

        Result result = launchMain(
                state,
                TOKEN,
                "com.lambda.Debugger.IntegrationInternalFailureHarness");

        assertEquals(result.stderr, 1, result.exitCode);
        assertTrue(result.stderr, result.stderr.contains("\"code\":\"INTERNAL_ERROR\""));
    }

    private Result launch(Path state, String token, String... arguments) throws Exception {
        return launchMain(state, token, "com.lambda.Debugger.IntegrationHeadlessLauncherHarness", arguments);
    }

    private Result launchMain(Path state, String token, String mainClass, String... arguments) throws Exception {
        return launchProcess(state.toString(), token, new String[0], mainClass, arguments);
    }

    private Result launchRaw(
            String state,
            String token,
            String[] vmOptions,
            String... arguments) throws Exception {
        return launchProcess(state, token, vmOptions, "com.lambda.Debugger.IntegrationLauncher", arguments);
    }

    private Result launchProcess(
            String state,
            String token,
            String[] vmOptions,
            String mainClass,
            String... arguments) throws Exception {
        List<String> command = new ArrayList<String>();
        command.add(javaExecutable());
        if (state != null) {
            command.add("-Dcom.lambda.Debugger.integration.stateDir=" + state);
        }
        if (token != null) {
            command.add("-Dcom.lambda.Debugger.integration.token=" + token);
        }
        command.addAll(Arrays.asList(vmOptions));
        command.add("-Djava.awt.headless=true");
        command.add("-cp");
        command.add(System.getProperty("java.class.path"));
        command.add(mainClass);
        command.addAll(Arrays.asList(arguments));
        Path workingDirectory = Files.createTempDirectory("odb-integration-work");
        Process process = new ProcessBuilder(command).directory(workingDirectory.toFile()).start();
        if (!process.waitFor(10, TimeUnit.SECONDS)) {
            process.destroyForcibly();
            process.waitFor(5, TimeUnit.SECONDS);
            fail("integration process timed out");
        }
        return new Result(
                process.exitValue(),
                read(process.getInputStream()),
                read(process.getErrorStream()),
                workingDirectory);
    }

    private static String javaExecutable() {
        return new File(new File(System.getProperty("java.home"), "bin"), "java").getAbsolutePath();
    }

    private static String read(InputStream input) throws IOException {
        ByteArrayOutputStream output = new ByteArrayOutputStream();
        byte[] buffer = new byte[4096];
        int count;
        while ((count = input.read(buffer)) >= 0) {
            output.write(buffer, 0, count);
        }
        return new String(output.toByteArray(), StandardCharsets.UTF_8);
    }

    private static void assertOrdered(String value, String... markers) {
        int previous = -1;
        for (String marker : markers) {
            int current = value.indexOf(marker);
            assertTrue(value, current > previous);
            previous = current;
        }
    }

    private static int occurrences(String value, String marker) {
        int count = 0;
        int offset = 0;
        while ((offset = value.indexOf(marker, offset)) >= 0) {
            count++;
            offset += marker.length();
        }
        return count;
    }

    private static final class Result {
        final int exitCode;
        final String stdout;
        final String stderr;
        final Path workingDirectory;

        Result(int exitCode, String stdout, String stderr, Path workingDirectory) {
            this.exitCode = exitCode;
            this.stdout = stdout;
            this.stderr = stderr;
            this.workingDirectory = workingDirectory;
        }
    }
}
