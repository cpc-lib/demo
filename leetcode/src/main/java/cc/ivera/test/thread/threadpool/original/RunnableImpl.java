package cc.ivera.test.thread.threadpool.original;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 11/28/2021 4:20 PM
 */
public class RunnableImpl implements Runnable {
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "正在进行运行......");
    }
}
