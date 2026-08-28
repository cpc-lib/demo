package com.example.distributedlock.web;

import com.example.distributedlock.lock.LockType;
import com.example.distributedlock.service.ConcurrentDemoService;
import com.example.distributedlock.service.InventoryService;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.LinkedHashMap;
import java.util.Map;

@Validated
@RestController
@RequestMapping("/api/demo")
public class DistributedLockDemoController {

    private final InventoryService inventoryService;
    private final ConcurrentDemoService concurrentDemoService;

    public DistributedLockDemoController(
            InventoryService inventoryService,
            ConcurrentDemoService concurrentDemoService) {
        this.inventoryService = inventoryService;
        this.concurrentDemoService = concurrentDemoService;
    }

    @PostMapping("/inventory/reset")
    public Map<String, Object> reset(
            @RequestParam(defaultValue = "1001") long productId,
            @RequestParam(defaultValue = "20") @Min(0) int stock) {
        inventoryService.reset(productId, stock);
        return Map.of("productId", productId, "stock", stock);
    }

    @GetMapping("/inventory/{productId}")
    public Map<String, Object> stock(@PathVariable long productId) {
        return Map.of("productId", productId, "stock", inventoryService.getStock(productId));
    }

    @PostMapping("/decrement/{provider}")
    public Map<String, Object> decrement(
            @PathVariable String provider,
            @RequestParam(defaultValue = "1001") long productId) {
        LockType type = LockType.from(provider);
        boolean success = inventoryService.decrementWithLock(type, productId);

        Map<String, Object> result = new LinkedHashMap<>();
        result.put("provider", type);
        result.put("success", success);
        result.put("productId", productId);
        result.put("stock", inventoryService.getStock(productId));
        result.put("message", success ? "扣减成功" : "库存不足");
        return result;
    }

    @PostMapping("/concurrent/{provider}")
    public ConcurrentDemoService.ConcurrentResult concurrent(
            @PathVariable String provider,
            @RequestParam(defaultValue = "1001") long productId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int initialStock,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int requests) {
        return concurrentDemoService.run(LockType.from(provider), productId, initialStock, requests);
    }

    @PostMapping("/concurrent-unsafe")
    public ConcurrentDemoService.ConcurrentResult concurrentUnsafe(
            @RequestParam(defaultValue = "1001") long productId,
            @RequestParam(defaultValue = "20") @Min(1) @Max(100) int initialStock,
            @RequestParam(defaultValue = "20") @Min(1) @Max(200) int requests) {
        return concurrentDemoService.runUnsafe(productId, initialStock, requests);
    }
}
