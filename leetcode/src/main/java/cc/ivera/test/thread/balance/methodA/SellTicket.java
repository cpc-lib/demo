package cc.ivera.test.thread.balance.methodA;

import java.util.concurrent.locks.ReentrantLock;

/**
 * @author e2607
 * @version 1.0
 * @description: 模拟售票（使用lock锁来确保线程安全）
 * @date 11/27/2021 12:01 PM
 */
public class SellTicket implements Runnable {

    private volatile int ticket = 90;

    //创建对象ReentrantLock时，传递参数true实现公平锁

    private ReentrantLock lock = new ReentrantLock(true);

    @Override
    public void run() {
        while (true) {
            lock.lock();
            try {
                if (ticket <= 0) {
                    break;
                }
                System.out.println(Thread.currentThread().getName() + "正在售票,还剩下" + (--ticket) + "张票");
                Thread.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            } finally {
                lock.unlock();
            }
        }
    }
}
