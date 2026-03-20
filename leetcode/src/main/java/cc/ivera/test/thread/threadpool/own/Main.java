package cc.ivera.test.thread.threadpool.own;

import java.util.concurrent.ArrayBlockingQueue;
import java.util.concurrent.Executors;
import java.util.concurrent.ThreadPoolExecutor;
import java.util.concurrent.TimeUnit;

/**
 * @author e2607
 * @version 1.0
 * @description: TODO.md
 * @date 11/28/2021 6:07 PM
 */
public class Main {
    public static void main(String[] args) {
        int i = Runtime.getRuntime().availableProcessors();
        System.out.println(i);


        /**
         * 线程池的作用:
         * 降低资源消耗。通过重复利用已创建的线程降低线程创建、销毁线程造成的消耗。
         *
         * 提高响应速度。当任务到达时，任务可以不需要等到线程创建就能立即执行。
         *
         * 提高线程的可管理性。线程是稀缺资源，如果无限制的创建，不仅会消耗系统资源，还会降低系统的稳定性，使用线程池可以进行统一的分配、调优和监控
         */

        //七个参数
        /**
         * corePoolSize	核心线程数量，线程池维护线程的最少数量
         * maximumPoolSize	线程池维护线程的最大数量
         * keepAliveTime	线程池除核心线程外的其他线程的最长空闲时间，超过该时间的空闲线程会被销毁
         * unit	keepAliveTime的单位，TimeUnit中的几个静态属性：NANOSECONDS、MICROSECONDS、MILLISECONDS、SECONDS
         * workQueue	线程池所使用的任务缓冲队列
         * threadFactory	线程工厂，用于创建线程，一般用默认的即可
         * handler	线程池对拒绝任务的处理策略
         */

        /***
         *ArrayBlockingQueue： 这是一个由数组实现的容量固定的有界阻塞队列.
         * SynchronousQueue： 没有容量，不能缓存数据；每个put必须等待一个take; offer()的时候如果没有另一个线程在poll()或者take()的话返回false。
         * LinkedBlockingQueue： 这是一个由单链表实现的默认无界的阻塞队列。LinkedBlockingQueue提供了一个可选有界的构造函数，而在未指明容量时，容量默认为Integer.MAX_VALUE
         */
        //四种拒绝策略
        /**
         * ThreadPoolExecutor.AbortPolicy(默认):丢弃任务并抛出RejectedExecutionException异常；也是默认的处理方式。
         * ThreadPoolExecutor.DiscardPolicy：丢弃任务，但是不抛出异常。
         * ThreadPoolExecutor.DiscardOldestPolicy：丢弃队列最前面的任务，然后重新尝试执行任务（重复此过程）
         * ThreadPoolExecutor.CallerRunsPolicy：由调用线程处理该任务
         */


        ThreadPoolExecutor threadPoolExecutor = new ThreadPoolExecutor(i, 2 * i, 5,
                TimeUnit.SECONDS, new ArrayBlockingQueue<>(10),
                Executors.defaultThreadFactory(), new ThreadPoolExecutor.AbortPolicy());

        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());
        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());
        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());
        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());

        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());
        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());
        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());
        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());

        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());
        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());
        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());
        threadPoolExecutor.submit(new TaskA());
        threadPoolExecutor.submit(new TaskB());

    }
}
