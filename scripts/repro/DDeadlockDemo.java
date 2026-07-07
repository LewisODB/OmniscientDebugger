import java.lang.management.ManagementFactory;
import java.lang.management.ThreadInfo;
import java.lang.management.ThreadMXBean;
import java.util.concurrent.CountDownLatch;

import com.lambda.Debugger.D;
import com.lambda.Debugger.DebugifyingClassLoader;

public class DDeadlockDemo {
    public static void main(String[] args) throws Exception {
        Object loader = new DebugifyingClassLoader();
        CountDownLatch loaderHasLock = new CountDownLatch(1);
        CountDownLatch dHasLock = new CountDownLatch(1);
        CountDownLatch release = new CountDownLatch(1);

        Thread loaderThread = new Thread(() -> {
            synchronized (loader) {
                loaderHasLock.countDown();
                await(dHasLock);
                await(release);
                synchronized (D.class) {
                    System.out.println("unreachable");
                }
            }
        }, "DebugifyingClassLoader -> D.class");

        Thread dThread = new Thread(() -> {
            synchronized (D.class) {
                dHasLock.countDown();
                await(loaderHasLock);
                release.countDown();
                synchronized (loader) {
                    System.out.println("unreachable");
                }
            }
        }, "old D.java -> Class.getFields -> DebugifyingClassLoader");

        loaderThread.setDaemon(true);
        dThread.setDaemon(true);
        loaderThread.start();
        dThread.start();

        ThreadMXBean bean = ManagementFactory.getThreadMXBean();
        for (int i = 0; i < 120; i++) {
            long[] ids = bean.findDeadlockedThreads();
            if (ids != null) {
                System.out.println("Deadlock detected:");
                for (ThreadInfo info : bean.getThreadInfo(ids, true, true)) {
                    System.out.println(info.getThreadName() + " waiting on " + info.getLockName());
                }
                System.exit(0);
                return;
            }
            Thread.sleep(25);
        }

        throw new IllegalStateException("deadlock not detected");
    }

    private static void await(CountDownLatch latch) {
        try {
            latch.await();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError(e);
        }
    }
}
