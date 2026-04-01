package cc.ivera.aba;

import java.util.concurrent.atomic.AtomicStampedReference;

/**
 * 解决 ABA 问题：
 * 使用 AtomicStampedReference 给值附带版本号（stamp）。
 * 即使值从 A -> B -> A，版本号也会变化，
 * 这样旧线程再执行 CAS 时就能检测到中间发生过修改。
 */
public class AbaSolutionDemo {

    private static final AtomicStampedReference<Integer> REF =
            new AtomicStampedReference<>(100, 1);

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> {
            int stamp = REF.getStamp();
            Integer value = REF.getReference();

            System.out.println("t1 初始值: " + value + ", 初始版本号: " + stamp);

            sleep(1000);

            boolean result = REF.compareAndSet(100, 200, stamp, stamp + 1);
            System.out.println("t1 CAS 结果: " + result);
            System.out.println("最终值: " + REF.getReference() + ", 最终版本号: " + REF.getStamp());
        }, "t1");

        Thread t2 = new Thread(() -> {
            int stamp1 = REF.getStamp();
            boolean step1 = REF.compareAndSet(100, 101, stamp1, stamp1 + 1);

            int stamp2 = REF.getStamp();
            boolean step2 = REF.compareAndSet(101, 100, stamp2, stamp2 + 1);

            System.out.println("t2 第一次修改结果: " + step1);
            System.out.println("t2 第二次修改结果: " + step2);
            System.out.println("t2 完成 ABA 操作，当前值: " + REF.getReference() + ", 当前版本号: " + REF.getStamp());
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
