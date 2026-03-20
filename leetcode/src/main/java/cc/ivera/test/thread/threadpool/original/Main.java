package cc.ivera.test.thread.threadpool.original;

import java.util.concurrent.ExecutorService;

import static java.util.concurrent.Executors.newFixedThreadPool;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 11/28/2021 4:21 PM
 */
public class Main {
    public static void main(String[] args) {
        //newFixedTreadPool的内部实现原理
        /*new ThreadPoolExecutor(0,Integer.MAX_VALUE,60L,TimeUnit.SECONDS,new SynchronousQueue())*/
        /**
         * public ThreadPoolExecutor(int corePoolSize,
         *                               int maximumPoolSize,
         *                               long keepAliveTime,
         *                               TimeUnit unit,
         *                               BlockingQueue<Runnable> workQueue,
         *                               ThreadFactory threadFactory,
         *                               RejectedExecutionHandler handler) {
         *         if (corePoolSize < 0 ||
         *             maximumPoolSize <= 0 ||
         *             maximumPoolSize < corePoolSize ||
         *             keepAliveTime < 0)
         *             throw new IllegalArgumentException();
         *         if (workQueue == null || threadFactory == null || handler == null)
         *             throw new NullPointerException();
         *         this.acc = System.getSecurityManager() == null ?
         *                 null :
         *                 AccessController.getContext();
         *         this.corePoolSize = corePoolSize;
         *         this.maximumPoolSize = maximumPoolSize;
         *         this.workQueue = workQueue;
         *         this.keepAliveTime = unit.toNanos(keepAliveTime);
         *         this.threadFactory = threadFactory;
         *         this.handler = handler;
         *     }
         */
        ExecutorService executorService = newFixedThreadPool(10);
        executorService.submit(new RunnableImpl());
        executorService.submit(new RunnableImpl());
        executorService.submit(new RunnableImpl());
        executorService.submit(new RunnableImpl());
        executorService.submit(new RunnableImpl());
        executorService.submit(new RunnableImpl());
        executorService.submit(new RunnableImpl());
        executorService.submit(new RunnableImpl());
        executorService.submit(new RunnableImpl());
        executorService.submit(new RunnableImpl());
        executorService.submit(new RunnableImpl());
        //线程任务执行完之后，线程归还到线程池中
        executorService.submit(new RunnableImpl());
        //线程关闭使用命令
        executorService.shutdown();//不推荐使用，关了还需要重建线程池来执行线程任务。
        //线程池关闭之后，提交任务，java.util.concurrent.RejectedExecutionException
        //executorService.submit(new RunnableImpl());
    }
}
