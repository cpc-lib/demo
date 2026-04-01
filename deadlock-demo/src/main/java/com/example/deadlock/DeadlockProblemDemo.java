package com.example.deadlock;

/**
 * 死锁案例：
 * t1 先持有 lockA，再尝试获取 lockB
 * t2 先持有 lockB，再尝试获取 lockA
 * 两个线程互相等待，形成死锁
 */
public class DeadlockProblemDemo {

    private static final Object LOCK_A = new Object();
    private static final Object LOCK_B = new Object();

    public static void main(String[] args) {
        Thread t1 = new Thread(() -> {
            synchronized (LOCK_A) {
                System.out.println("t1 获取到 LOCK_A");
                sleep(500);
                System.out.println("t1 尝试获取 LOCK_B");
                synchronized (LOCK_B) {
                    System.out.println("t1 获取到 LOCK_B");
                }
            }
        }, "t1");

        Thread t2 = new Thread(() -> {
            synchronized (LOCK_B) {
                System.out.println("t2 获取到 LOCK_B");
                sleep(500);
                System.out.println("t2 尝试获取 LOCK_A");
                synchronized (LOCK_A) {
                    System.out.println("t2 获取到 LOCK_A");
                }
            }
        }, "t2");

        t1.start();
        t2.start();
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
