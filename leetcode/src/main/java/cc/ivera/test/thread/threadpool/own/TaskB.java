package cc.ivera.test.thread.threadpool.own;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 11/28/2021 10:20 PM
 */
public class TaskB implements Runnable {
    @Override
    public void run() {
        System.out.println(Thread.currentThread().getName() + "下载图片");
    }
}
