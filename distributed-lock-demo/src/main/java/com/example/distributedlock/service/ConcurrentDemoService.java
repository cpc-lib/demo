package com.example.distributedlock.service;

import com.example.distributedlock.lock.DistributedLockExecutor;
import com.example.distributedlock.lock.LockType;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

@Service
public class ConcurrentDemoService {

    private final InventoryService inventoryService;

    public ConcurrentDemoService(InventoryService inventoryService) {
        this.inventoryService = inventoryService;
    }

    public ConcurrentResult run(LockType type, long productId, int initialStock, int requests) {
        inventoryService.reset(productId, initialStock);
        return runTasks(productId, requests, () -> inventoryService.decrementWithLock(type, productId), type.name());
    }

    public ConcurrentResult runUnsafe(long productId, int initialStock, int requests) {
        inventoryService.reset(productId, initialStock);
        return runTasks(productId, requests, () -> inventoryService.decrementUnsafe(productId), "NONE");
    }

    private ConcurrentResult runTasks(long productId, int requests, Callable<Boolean> task, String provider) {
        if (requests < 1 || requests > 200) {
            throw new IllegalArgumentException("requests must be between 1 and 200");
        }

        int poolSize = Math.min(requests, 32);
        long started = System.currentTimeMillis();
        int success = 0;
        int rejectedOrFailed = 0;

        try (ExecutorService executor = Executors.newFixedThreadPool(poolSize)) {
            List<Future<Boolean>> futures = new ArrayList<>(requests);
            for (int i = 0; i < requests; i++) {
                futures.add(executor.submit(task));
            }

            for (Future<Boolean> future : futures) {
                try {
                    if (Boolean.TRUE.equals(future.get())) {
                        success++;
                    } else {
                        rejectedOrFailed++;
                    }
                } catch (Exception e) {
                    rejectedOrFailed++;
                }
            }
        }

        return new ConcurrentResult(
                provider,
                productId,
                requests,
                success,
                rejectedOrFailed,
                inventoryService.getStock(productId),
                System.currentTimeMillis() - started
        );
    }

    public record ConcurrentResult(
            String provider,
            long productId,
            int requests,
            int successfulBusinessExecutions,
            int rejectedOrFailed,
            int finalStock,
            long elapsedMs) {
    }
}
