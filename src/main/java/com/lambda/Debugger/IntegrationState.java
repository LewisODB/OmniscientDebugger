package com.lambda.Debugger;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.PrintStream;
import java.nio.channels.Channels;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.InvalidPathException;
import java.nio.file.LinkOption;
import java.nio.file.NoSuchFileException;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardOpenOption;
import java.util.Arrays;

final class IntegrationState {
    static final String STATE_PROPERTY = "com.lambda.Debugger.integration.stateDir";
    static final String TOKEN_PROPERTY = "com.lambda.Debugger.integration.token";
    private static final String SOURCE_ROOTS_FILE = "source-roots.txt";
    private static final String PREFIX = "@@ODB-INTEGRATION@@\t";
    private static final String[] FORBIDDEN_PROPERTIES = {
        "DONT_INSTRUMENT",
        "DONT_START",
        "PAUSED",
        "DONT_SHOW",
        "NO_WINDOWS",
        "NO_DEFAULTS",
        "DEBUGIFY_ONLY",
        "DONT_KILL_TARGET",
    };
    private static IntegrationState active;

    private final PrintStream originalError;
    private final Path stateDirectory;
    private final String token;
    private long sequence;
    private boolean recordingStarted;
    private boolean debuggerReady;

    private IntegrationState(PrintStream originalError, Path stateDirectory, String token) {
        this.originalError = originalError;
        this.stateDirectory = stateDirectory;
        this.token = token;
    }

    static Launch start(PrintStream originalError, String[] args) throws BadContract {
        String stateValue = System.getProperty(STATE_PROPERTY);
        String tokenValue = System.getProperty(TOKEN_PROPERTY);
        System.clearProperty(STATE_PROPERTY);
        System.clearProperty(TOKEN_PROPERTY);

        if (args == null || args.length == 0 || args[0] == null || args[0].length() == 0) {
            throw new BadContract(tokenValue, "Expected a target main class.");
        }
        if (!isBinaryClassName(args[0])) {
            throw new BadContract(tokenValue, "Target main class must be a Java binary name.");
        }
        if (tokenValue == null || !tokenValue.matches("[0-9a-f]{32}")) {
            throw new BadContract(tokenValue, "Integration token must be 32 lowercase hexadecimal characters.");
        }
        Path stateDirectory = validateStateDirectory(stateValue, tokenValue);
        for (String property : FORBIDDEN_PROPERTIES) {
            if (System.getProperty(property) != null) {
                throw new BadContract(tokenValue, "Integration mode does not allow -D" + property + ".");
            }
        }

        IntegrationState state = new IntegrationState(originalError, stateDirectory, tokenValue);
        active = state;
        Launch launch = new Launch(args[0], Arrays.copyOfRange(args, 1, args.length));
        state.emit("runtime-ready", "\"target\":" + quote(launch.target));
        return launch;
    }

    private static Path validateStateDirectory(String value, String token) throws BadContract {
        if (value == null) {
            throw new BadContract(token, "Expected an absolute integration state directory.");
        }
        Path supplied;
        try {
            supplied = Paths.get(value);
        } catch (RuntimeException error) {
            throw new BadContract(token, "Integration state directory is invalid.");
        }
        if (!supplied.isAbsolute()) {
            throw new BadContract(token, "Integration state directory must be absolute.");
        }
        try {
            Path real = supplied.toRealPath();
            if (!Files.isDirectory(real) || !Files.isWritable(real)) {
                throw new BadContract(token, "Integration state directory must exist and be writable.");
            }
            return real;
        } catch (IOException error) {
            throw new BadContract(token, "Integration state directory must resolve to a writable directory.");
        }
    }

    private static boolean isBinaryClassName(String value) {
        boolean atSegmentStart = true;
        for (int offset = 0; offset < value.length();) {
            int character = value.codePointAt(offset);
            if (character == '.') {
                if (atSegmentStart) {
                    return false;
                }
                atSegmentStart = true;
            } else if (atSegmentStart) {
                if (!Character.isJavaIdentifierStart(character)) {
                    return false;
                }
                atSegmentStart = false;
            } else if (!Character.isJavaIdentifierPart(character)) {
                return false;
            }
            offset += Character.charCount(character);
        }
        return !atSegmentStart;
    }

    static boolean isActive() {
        return active != null;
    }

    static void loadSourceDirectories() {
        if (active == null) {
            return;
        }
        try (BufferedReader reader = new BufferedReader(new InputStreamReader(
                active.openInput(SOURCE_ROOTS_FILE), StandardCharsets.UTF_8))) {
            String value;
            while ((value = reader.readLine()) != null) {
                Path root;
                try {
                    root = Paths.get(value);
                } catch (InvalidPathException error) {
                    continue;
                }
                if (!root.isAbsolute()) {
                    continue;
                }
                root = root.normalize();
                if (!Files.isDirectory(root)) {
                    continue;
                }
                String directory = root.toString();
                if (!directory.endsWith(File.separator)) {
                    directory += File.separator;
                }
                if (!SourceFileFinder.sourceDirectories.contains(directory)) {
                    SourceFileFinder.sourceDirectories.add(directory);
                }
            }
        } catch (IOException ignored) {
            // Missing or unreadable source roots preserve ODB's chooser fallback.
        }
    }

    static String defaultsFile(String legacyPath) {
        return active == null ? legacyPath : active.stateDirectory.resolve(".debuggerDefaults").toString();
    }

    static String commandHistoryFile(String target) {
        String legacyPath = target + ".debuggerCommands";
        return active == null ? legacyPath : active.stateDirectory.resolve(legacyPath).toString();
    }

    static InputStream defaultsInput(String legacyPath) throws IOException {
        return active == null
                ? new FileInputStream(legacyPath)
                : active.openInput(".debuggerDefaults");
    }

    static OutputStream defaultsOutput(String legacyPath) throws IOException {
        return active == null
                ? new FileOutputStream(legacyPath)
                : active.openOutput(".debuggerDefaults");
    }

    static InputStream commandHistoryInput(String target) throws IOException {
        String filename = target + ".debuggerCommands";
        return active == null
                ? new FileInputStream(filename)
                : active.openInput(filename);
    }

    static OutputStream commandHistoryOutput(String target) throws IOException {
        String filename = target + ".debuggerCommands";
        return active == null
                ? new FileOutputStream(filename)
                : active.openOutput(filename);
    }

    private InputStream openInput(String filename) throws IOException {
        try {
            return Channels.newInputStream(Files.newByteChannel(
                    managedFile(filename),
                    StandardOpenOption.READ,
                    LinkOption.NOFOLLOW_LINKS));
        } catch (NoSuchFileException error) {
            FileNotFoundException missing = new FileNotFoundException(error.getFile());
            missing.initCause(error);
            throw missing;
        }
    }

    private OutputStream openOutput(String filename) throws IOException {
        return Channels.newOutputStream(Files.newByteChannel(
                managedFile(filename),
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING,
                StandardOpenOption.WRITE,
                LinkOption.NOFOLLOW_LINKS));
    }

    private Path managedFile(String filename) throws IOException {
        Path file = stateDirectory.resolve(filename).normalize();
        if (!stateDirectory.equals(file.getParent())) {
            throw new IOException("Managed ODB file escaped the integration state directory.");
        }
        return file;
    }

    static void targetLoaded(String target) {
        if (active != null) {
            active.emit("target-loaded", "\"target\":" + quote(target));
        }
    }

    static void requireUsefulRecording(long created, long retained) {
        if (active != null && retained < 2) {
            throw fatal(
                    "NO_RECORDING",
                    "ODB did not collect a usable recording.",
                    null,
                    "created=" + created + ", retained=" + retained,
                    1);
        }
    }

    static void timestampAdded(long created, long retained) {
        if (active != null) {
            active.recordingStarted(created, retained);
        }
    }

    static Error instrumentationFailed(String className, Throwable error) {
        return fatal(
                "INSTRUMENTATION_FAILED",
                "ODB could not instrument " + className + ".",
                className,
                error.toString(),
                1);
    }

    static Error stateIoFailed(String operation, Throwable error) {
        return fatal(
                "DEFAULTS_IO",
                "Could not " + operation + " in ODB integration state.",
                error.getClass().getName(),
                error.getMessage(),
                1);
    }

    static Error internalFailed(String message, Throwable error) {
        return fatal(
                "INTERNAL_ERROR",
                message,
                error.getClass().getName(),
                error.getMessage(),
                1);
    }

    private synchronized void recordingStarted(long created, long retained) {
        if (!recordingStarted && retained >= 2) {
            recordingStarted = true;
            emit("recording-started", "\"created\":" + created + ",\"retained\":" + retained);
        }
    }

    static void debuggerReady(long created, long retained) {
        requireUsefulRecording(created, retained);
        if (active != null) {
            active.emitDebuggerReady(created, retained);
        }
    }

    private synchronized void emitDebuggerReady(long created, long retained) {
        recordingStarted(created, retained);
        if (!debuggerReady) {
            debuggerReady = true;
            emit("debugger-ready", "\"created\":" + created + ",\"retained\":" + retained);
        }
    }

    static Error badContract(PrintStream error, String token, String message) {
        PrintStream destination = error == null ? System.err : error;
        String safeToken = token != null && token.matches("[0-9a-f]{32}") ? token : "";
        destination.println(PREFIX + safeToken + "\t{\"version\":1,\"sequence\":1,\"type\":\"fatal\","
                + "\"code\":\"BAD_CONTRACT\",\"message\":" + quote(message) + "}");
        destination.flush();
        Runtime.getRuntime().halt(2);
        return new AssertionError(message);
    }

    static Error fatal(String code, String message, String errorClass, String cause, int exitCode) {
        IntegrationState state = active;
        if (state == null) {
            return new AssertionError(message);
        }
        StringBuilder fields = new StringBuilder();
        fields.append("\"code\":").append(quote(code));
        fields.append(",\"message\":").append(quote(message));
        if (errorClass != null) {
            fields.append(",\"class\":").append(quote(errorClass));
        }
        if (cause != null) {
            fields.append(",\"cause\":").append(quote(cause));
        }
        state.emit("fatal", fields.toString());
        Runtime.getRuntime().halt(exitCode);
        return new AssertionError(message);
    }

    private synchronized void emit(String type, String additionalFields) {
        sequence++;
        originalError.println(PREFIX + token + "\t{\"version\":1,\"sequence\":" + sequence
                + ",\"type\":" + quote(type) + "," + additionalFields + "}");
        originalError.flush();
    }

    private static String quote(String value) {
        StringBuilder escaped = new StringBuilder("\"");
        for (int index = 0; index < value.length(); index++) {
            char character = value.charAt(index);
            switch (character) {
                case '\"':
                    escaped.append("\\\"");
                    break;
                case '\\':
                    escaped.append("\\\\");
                    break;
                case '\b':
                    escaped.append("\\b");
                    break;
                case '\f':
                    escaped.append("\\f");
                    break;
                case '\n':
                    escaped.append("\\n");
                    break;
                case '\r':
                    escaped.append("\\r");
                    break;
                case '\t':
                    escaped.append("\\t");
                    break;
                default:
                    if (character < 0x20 || character > 0x7e) {
                        escaped.append(String.format("\\u%04x", (int) character));
                    } else {
                        escaped.append(character);
                    }
            }
        }
        return escaped.append('\"').toString();
    }

    static final class Launch {
        final String target;
        final String[] arguments;

        Launch(String target, String[] arguments) {
            this.target = target;
            this.arguments = arguments;
        }
    }

    static final class BadContract extends Exception {
        final String token;

        BadContract(String token, String message) {
            super(message);
            this.token = token;
        }
    }
}
