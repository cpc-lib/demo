package cc.ivera.test.thread.threadpool.own;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 11/28/2021 10:18 PM
 */
public class TaskA implements Runnable {
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "传输文件");
    }
}
