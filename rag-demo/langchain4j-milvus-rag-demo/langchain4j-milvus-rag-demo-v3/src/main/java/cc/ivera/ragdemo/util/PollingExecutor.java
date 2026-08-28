package cc.ivera.ragdemo.util;

import lombok.extern.slf4j.Slf4j;

import java.util.concurrent.*;

/**
 * 非阻塞轮询执行器工具类
 * 
 * 使用 ScheduledExecutorService 实现异步轮询，避免 Thread.sleep 阻塞当前线程，
 * 提高系统在高并发场景下的吞吐量。
 */
@Slf4j
public class PollingExecutor {

    /**
     * 轮询任务状态枚举
     */
    public enum PollingStatus {
        RUNNING,    // 任务运行中
        SUCCEEDED,  // 任务成功
        FAILED,     // 任务失败
        TIMEOUT     // 任务超时
    }

    /**
     * 轮询结果封装
     */
    public static class PollingResult<T> {
        private final PollingStatus status;
        private final T result;
        private final Throwable error;

        private PollingResult(PollingStatus status, T result, Throwable error) {
            this.status = status;
            this.result = result;
            this.error = error;
        }

        public static <T> PollingResult<T> success(T result) {
            return new PollingResult<>(PollingStatus.SUCCEEDED, result, null);
        }

        public static <T> PollingResult<T> failure(Throwable error) {
            return new PollingResult<>(PollingStatus.FAILED, null, error);
        }

        public static <T> PollingResult<T> timeout() {
            return new PollingResult<>(PollingStatus.TIMEOUT, null, null);
        }

        public PollingStatus getStatus() {
            return status;
        }

        public T getResult() {
            return result;
        }

        public Throwable getError() {
            return error;
        }

        public boolean isSuccess() {
            return status == PollingStatus.SUCCEEDED;
        }

        public boolean isFailed() {
            return status == PollingStatus.FAILED;
        }

        public boolean isTimeout() {
            return status == PollingStatus.TIMEOUT;
        }
    }

    /**
     * 轮询回调接口
     * @param <T> 轮询结果类型
     */
    public interface PollingCallback<T> {
        /**
         * 执行一次轮询检查
         * @return 轮询结果，如果返回 null 表示继续轮询
         * @throws Exception 轮询过程中的异常
         */
        PollingResult<T> poll() throws Exception;
    }

    /**
     * 共享的单例 ScheduledExecutorService
     * 使用 Daemon 线程，避免影响应用关闭
     */
    private static final ScheduledExecutorService SCHEDULED_EXECUTOR = Executors.newScheduledThreadPool(
            Runtime.getRuntime().availableProcessors() + 1,
            r -> {
                Thread t = new Thread(r, "polling-executor");
                t.setDaemon(true);
                return t;
            }
    );

    /**
     * 执行异步轮询
     * 
     * @param callback 轮询回调函数
     * @param pollIntervalMillis 轮询间隔（毫秒）
     * @param timeoutSeconds 超时时间（秒）
     * @param <T> 结果类型
     * @return CompletableFuture 包装的轮询结果
     */
    public static <T> CompletableFuture<T> pollAsync(PollingCallback<T> callback, 
                                                      long pollIntervalMillis, 
                                                      long timeoutSeconds) {
        CompletableFuture<T> future = new CompletableFuture<>();
        long deadline = System.currentTimeMillis() + TimeUnit.SECONDS.toMillis(timeoutSeconds);
        
        // 创建轮询任务
        Runnable pollTask = new Runnable() {
            @Override
            public void run() {
                // 检查是否超时
                if (System.currentTimeMillis() > deadline) {
                    future.completeExceptionally(new TimeoutException("Polling timeout after " + timeoutSeconds + " seconds"));
                    return;
                }

                try {
                    PollingResult<T> result = callback.poll();
                    
                    if (result.isSuccess()) {
                        future.complete(result.getResult());
                    } else if (result.isFailed()) {
                        future.completeExceptionally(result.getError());
                    } else {
                        // 继续轮询
                        scheduleNextPoll(this, pollIntervalMillis);
                    }
                } catch (Exception e) {
                    log.warn("Polling callback threw exception, continuing: {}", e.getMessage());
                    scheduleNextPoll(this, pollIntervalMillis);
                }
            }

            private void scheduleNextPoll(Runnable task, long interval) {
                if (!future.isDone()) {
                    SCHEDULED_EXECUTOR.schedule(task, interval, TimeUnit.MILLISECONDS);
                }
            }
        };

        // 立即执行第一次轮询
        SCHEDULED_EXECUTOR.submit(pollTask);

        return future;
    }

    /**
     * 执行同步轮询（带超时）
     * 
     * 使用内部线程池执行轮询，当前线程通过 CompletableFuture.get() 等待结果，
     * 但不会占用调用线程进行实际轮询操作。
     * 
     * @param callback 轮询回调函数
     * @param pollIntervalMillis 轮询间隔（毫秒）
     * @param timeoutSeconds 超时时间（秒）
     * @param <T> 结果类型
     * @return 轮询结果
     * @throws InterruptedException 线程中断异常
     * @throws ExecutionException 执行异常
     * @throws TimeoutException 超时异常
     */
    public static <T> T pollSync(PollingCallback<T> callback,
                                  long pollIntervalMillis,
                                  long timeoutSeconds) throws InterruptedException, ExecutionException, TimeoutException {
        CompletableFuture<T> future = pollAsync(callback, pollIntervalMillis, timeoutSeconds);
        return future.get(timeoutSeconds + 5, TimeUnit.SECONDS);
    }

    /**
     * 执行同步轮询（带超时和异常处理）
     * 
     * @param callback 轮询回调函数
     * @param pollIntervalMillis 轮询间隔（毫秒）
     * @param timeoutSeconds 超时时间（秒）
     * @param errorMessage 错误消息前缀
     * @param <T> 结果类型
     * @return 轮询结果
     * @throws IllegalStateException 轮询失败或超时时抛出
     */
    public static <T> T pollSyncWithError(PollingCallback<T> callback,
                                           long pollIntervalMillis,
                                           long timeoutSeconds,
                                           String errorMessage) {
        try {
            return pollSync(callback, pollIntervalMillis, timeoutSeconds);
        } catch (TimeoutException e) {
            throw new IllegalStateException(errorMessage + " - timeout after " + timeoutSeconds + " seconds");
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new IllegalStateException(errorMessage + " - interrupted", e);
        } catch (ExecutionException e) {
            throw new IllegalStateException(errorMessage, e.getCause());
        }
    }

    /**
     * 关闭轮询执行器（仅用于测试或应用关闭时）
     */
    public static void shutdown() {
        SCHEDULED_EXECUTOR.shutdown();
        try {
            if (!SCHEDULED_EXECUTOR.awaitTermination(5, TimeUnit.SECONDS)) {
                SCHEDULED_EXECUTOR.shutdownNow();
            }
        } catch (InterruptedException e) {
            SCHEDULED_EXECUTOR.shutdownNow();
            Thread.currentThread().interrupt();
        }
    }
}
