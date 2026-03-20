package cc.ivera.test.base;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.TtlRunnable;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

//在使用线程池的情况下使用 TransmittableThreadLocal

public class Demo76 {
    private static TransmittableThreadLocal<String> threadLocal = new TransmittableThreadLocal<>();

    public static void main(String[] args) throws InterruptedException {
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // 在主线程中设置TransmittableThreadLocal的值
        threadLocal.set("Hello, World!");

        // 在线程池中执行任务
        executorService.execute(TtlRunnable.get(() -> {
            String value = threadLocal.get();
            System.out.println("TransmittableThreadLocal value in new thread: " + value);
        }));

        // 等待任务执行完成
        executorService.shutdown();
        executorService.awaitTermination(1, TimeUnit.SECONDS);
    }
}