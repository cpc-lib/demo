package cc.ivera.test.base;

import com.alibaba.ttl.TransmittableThreadLocal;
import com.alibaba.ttl.threadpool.TtlExecutors;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class Demo77 {
    private static TransmittableThreadLocal<String> threadLocal = new TransmittableThreadLocal<>();

    public static void main(String[] args) {
        // 创建一个固定大小的线程池
        ExecutorService executorService = Executors.newFixedThreadPool(2);

        // 在主线程中设置TransmittableThreadLocal的值
        threadLocal.set("Hello, World!");

        // 使用TtlExecutors包装线程池
        ExecutorService ttlExecutorService = TtlExecutors.getTtlExecutorService(executorService);

        // 提交任务到线程池中
        ttlExecutorService.submit(() -> {
            String value = threadLocal.get();
            System.out.println("Task is running in thread: " + Thread.currentThread().getName() + ", " +
                    "TransmittableThreadLocal value: " + value);
        });

        // 关闭线程池
        ttlExecutorService.shutdown();
    }
}