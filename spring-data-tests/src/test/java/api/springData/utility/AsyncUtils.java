package api.springData.utility;

import java.util.concurrent.CountDownLatch;

public class AsyncUtils {

    private static Runnable withCountDownLatch(Runnable task, CountDownLatch countDownLatch) {
        return () -> {
            try {
                countDownLatch.await();
                task.run();
            } catch (InterruptedException e) {
                throw new RuntimeException("Thread is interrupted", e);
            }
        };
    }
}
