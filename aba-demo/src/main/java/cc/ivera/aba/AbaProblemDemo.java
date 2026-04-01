package cc.ivera.aba;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * ABA 问题演示：
 * 线程 t1 先读取到 100，线程 t2 把值从 100 改成 101，再改回 100。
 * 最终 t1 再执行 compareAndSet(100, 200) 时会成功，
 * 但它并不知道这个值在中间被其他线程改动过。
 */
public class AbaProblemDemo {

    private static final AtomicInteger ATOMIC_INTEGER = new AtomicInteger(100);

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            int value = ATOMIC_INTEGER.get();
            System.out.println("t1 读取初始值: " + value);

            sleep(1000);

            boolean result = ATOMIC_INTEGER.compareAndSet(100, 200);
            System.out.println("t1 CAS 结果: " + result);
            System.out.println("最终值: " + ATOMIC_INTEGER.get());
        }, "t1");

        Thread t2 = new Thread(() -> {
            boolean step1 = ATOMIC_INTEGER.compareAndSet(100, 101);
            boolean step2 = ATOMIC_INTEGER.compareAndSet(101, 100);

            System.out.println("t2 第一次修改结果: " + step1);
            System.out.println("t2 第二次修改结果: " + step2);
            System.out.println("t2 完成 ABA 操作，当前值: " + ATOMIC_INTEGER.get());
        }, "t2");

        t1.start();
        Thread.sleep(100);
        t2.start();

        t1.join();
        t2.join();
    }

    private static void sleep(long millis) {
        try {
            Thread.sleep(millis);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new RuntimeException(e);
        }
    }
}
