package outside;

import java.io.File;
import java.io.FileOutputStream;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;

public final class BehaviorTarget {
    public static void main(String[] args) throws Exception {
        System.setErr(new PrintStream(new FileOutputStream("redirected-stderr.txt"), true, "UTF-8"));
        System.out.println("args=" + Arrays.toString(args));
        System.out.println("user.dir=" + System.getProperty("user.dir"));
        System.out.println("cwd=" + new File(".").getCanonicalPath());
        System.out.println("classpath-helper=" + TargetClasspathHelper.value());
        System.out.println("state-property=" + System.getProperty("com.lambda.Debugger.integration.stateDir"));
        System.out.println("token-property=" + System.getProperty("com.lambda.Debugger.integration.token"));
        Files.write(Paths.get("target-relative.txt"), "kept".getBytes(StandardCharsets.UTF_8));
        Runtime.getRuntime().halt(23);
    }
}
