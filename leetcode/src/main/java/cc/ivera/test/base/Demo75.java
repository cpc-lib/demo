package cc.ivera.test.base;

import com.alibaba.ttl.TransmittableThreadLocal;

/**
 * TransmittableThreadLocal是阿里巴巴开源的一个线程本地变量，
 * 是ThreadLocal的一个增强版，可以在线程池等多线程环境下使用，
 * 解决了ThreadLocal在多线程环境下的一些问题。
 * 在多线程环境下，ThreadLocal可以避免线程安全问题，
 * 但是在使用线程池等多线程环境时，ThreadLocal可能会出现一些问题。
 * 例如，当使用线程池时，线程池中的线程可能会被多个任务共享，如果使用ThreadLocal存储数据，可能会导致数据被错误地共享。
 * TransmittableThreadLocal解决了这个问题，它可以在多线程环境下正确地传递数据。
 * 具体来说，当一个线程从线程池中获取到一个线程时，TransmittableThreadLocal会自动将当前线程的ThreadLocal副本传递给新的线程，从而保证数据的正确性。
 */
//https://gitcode.csdn.net/65ea83d51a836825ed793619.html
//直接使用 TransmittableThreadLocal

public class Demo75 {
    private static TransmittableThreadLocal<String> threadLocal = new TransmittableThreadLocal<>();

    public static void main(String[] args) {
        // 在主线程中设置TransmittableThreadLocal的值
        threadLocal.set("Hello, World!");

        // 在新线程中获取TransmittableThreadLocal的值
        Thread thread = new Thread(() -> {
            String value = threadLocal.get();
            System.out.println("TransmittableThreadLocal value in new thread: " + value);
        });
        thread.start();
    }
}