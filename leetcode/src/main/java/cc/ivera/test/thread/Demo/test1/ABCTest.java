package cc.ivera.test.thread.Demo.test1;

import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class ABCTest {

    static int  num = 1;

    public static void main(String[] args) {

        Lock lock = new ReentrantLock();

        Condition conditionA = lock.newCondition();
        Condition conditionB = lock.newCondition();
        Condition conditionC = lock.newCondition();

        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    for (int i = 0; i < 10; i++) {
                        while(num % 3 != 1) {
                            conditionC.await();
                        }

                        System.out.println("A");
                        num++;
                        conditionA.signal();
                    }
                } catch (Exception e) {

                } finally {
                    lock.unlock();
                }
            }
        }, "A").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    for (int i = 0; i < 10; i++) {
                        while(num % 3 != 2) {
                            conditionA.await();
                        }

                        System.out.println("B");
                        num++;
                        conditionB.signal();
                    }
                } catch (Exception e) {

                } finally {
                    lock.unlock();
                }
            }
        }, "B").start();

        new Thread(new Runnable() {
            @Override
            public void run() {
                lock.lock();
                try {
                    for (int i = 0; i < 10; i++) {
                        while (num % 3 != 0) {
                            conditionB.await();
                        }

                        System.out.println("C");
                        num++;
                        conditionC.signal();
                    }
                } catch (Exception e) {

                } finally {
                    lock.unlock();
                }
            }
        }, "C").start();
    }
}
