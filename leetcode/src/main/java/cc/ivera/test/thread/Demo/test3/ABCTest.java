package cc.ivera.test.thread.Demo.test3;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.ReentrantLock;

public class ABCTest {
    private static final int MAX_NUM = 30;
    private static int count = 1;
    private static final ReentrantLock lock = new ReentrantLock();
    private static final Condition[] conditions = new Condition[3];
    
    static {
        for (int i = 0; i < 3; i++) {
            conditions[i] = lock.newCondition();
        }
    }

    static class PrintTask implements Runnable {
        private final int threadId;
        
        public PrintTask(int threadId) {
            this.threadId = threadId;
        }

        @Override
        public void run() {
            while (count <= MAX_NUM) {
                lock.lock();
                try {
                    // 检查是否轮到自己执行
                    while (count % 3 != threadId && count <= MAX_NUM) {
                        conditions[threadId].await();
                    }
                    
                    if (count <= MAX_NUM) {
                        System.out.println("Thread-" + (threadId + 1) + ": " + count++);
                    }
                    
                    // 唤醒下一个线程
                    conditions[(threadId + 1) % 3].signal();
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    lock.unlock();
                }
            }
        }
    }

    public static void main(String[] args) {
        new Thread(new PrintTask(1), "Thread-2").start();
        new Thread(new PrintTask(2), "Thread-3").start();
        new Thread(new PrintTask(0), "Thread-1").start();  // 最后启动Thread-1
    }
}
