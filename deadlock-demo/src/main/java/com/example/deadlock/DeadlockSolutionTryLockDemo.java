package com.example.deadlock;

import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.ReentrantLock;

/**
 * 解决方案 2：
 * 使用 ReentrantLock + tryLock + 超时机制
 * 获取不到第二把锁时主动释放已持有的锁并重试，避免永久阻塞
 */
public class DeadlockSolutionTryLockDemo {

    private static final ReentrantLock LOCK_A = new ReentrantLock();
    private static final ReentrantLock LOCK_B = new ReentrantLock();

    public static void main(String[] args) throws InterruptedException {
        Thread t1 = new Thread(() -> doWork("t1", LOCK_A, LOCK_B), "t1");
        Thread t2 = new Thread(() -> doWork("t2", LOCK_B, LOCK_A), "t2");

        t1.start();
        t2.start();

        t1.join();
        t2.join();

        System.out.println("程序执行结束，没有发生死锁");
    }

    private static void doWork(String threadName, ReentrantLock first, ReentrantLock second) {
        while (true) {
            boolean gotFirst = false;
            boolean gotSecond = false;
            try {
                gotFirst = first.tryLock(1, TimeUnit.SECONDS);
                if (!gotFirst) {
                    System.out.println(threadName + " 获取第一把锁失败，准备重试");
                    continue;
                }

                System.out.println(threadName + " 获取到第一把锁");
                sleep(200);

                gotSecond = second.tryLock(1, TimeUnit.SECONDS);
                if (!gotSecond) {
                    System.out.println(threadName + " 获取第二把锁失败，释放第一把锁后重试");
                    continue;
                }

                System.out.println(threadName + " 获取到第二把锁");
                System.out.println(threadName + " 执行业务逻辑");
                break;
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                throw new RuntimeException(e);
            } finally {
                if (gotSecond) {
                    second.unlock();
                }
                if (gotFirst) {
                    first.unlock();
                }
            }
        }
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
