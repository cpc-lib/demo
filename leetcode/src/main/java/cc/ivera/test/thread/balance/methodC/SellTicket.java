package cc.ivera.test.thread.balance.methodC;


import java.util.concurrent.locks.ReentrantLock;

/**
 * @author e2607
 * @version 1.0
 * @description: 模拟售票（使用lock锁加volatile的特性来实现公平消费）
 * @date 11/27/2021 12:01 PM
 */
public class SellTicket /*implements Runnable*/ {

    private volatile static int ticket = 90;
    private static ReentrantLock lock = new ReentrantLock();
    private volatile static int index = 0;

    public static void main(String[] args) {
        Thread thread1 = new Thread(() -> {
            while (ticket > 0) {
                //volatile只能保证可见性，因此lock.lock()代码需写在条件判断之前
                lock.lock();
                if (index % 3 == 0) {
                    if (ticket > 0) {
                        System.out.println(Thread.currentThread().getName() + "正在售票,还剩下" + (--ticket) + "张票");
                    }
                    index++;
                }
                lock.unlock();
            }
        });
        Thread thread2 = new Thread(() -> {
            while (ticket > 0) {
                lock.lock();
                if (index % 3 == 1) {
                    if (ticket > 0) {
                        System.out.println(Thread.currentThread().getName() + "正在售票,还剩下" + (--ticket) + "张票");
                    }

                    index++;
                }
                lock.unlock();
            }

        });
        Thread thread3 = new Thread(() -> {
            while (ticket > 0) {
                lock.lock();
                if (index % 3 == 2) {
                    if (ticket > 0) {
                        System.out.println(Thread.currentThread().getName() + "正在售票,还剩下" + (--ticket) + "张票");
                    }
                    index++;
                }
                lock.unlock();
            }
        });
        thread1.start();
        thread2.start();
        thread3.start();
    }

}
