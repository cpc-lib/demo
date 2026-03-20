package cc.ivera.test.juc._06_ThreadPool_gettingStarted;

import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

public class _01_ThreadPool {
    public static void main(String[] args) {

        Executors.newSingleThreadExecutor();

        Executors.newCachedThreadPool();

        ExecutorService executorService = Executors.newFixedThreadPool(3);
//        executorService.

    }
}
