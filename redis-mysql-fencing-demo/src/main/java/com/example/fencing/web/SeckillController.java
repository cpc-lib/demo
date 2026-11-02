package com.example.fencing.web;

import com.example.fencing.model.ProductSnapshot;
import com.example.fencing.model.SeckillResetResponse;
import com.example.fencing.model.SeckillResponse;
import com.example.fencing.repository.ProductRepository;
import com.example.fencing.service.SeckillCoordinator;
import com.example.fencing.service.SeckillResetService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.constraints.Pattern;
import java.util.List;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/seckill")
public class SeckillController {
    private final SeckillCoordinator coordinator;
    private final SeckillResetService resetter;
    private final ProductRepository products;

    public SeckillController(SeckillCoordinator coordinator, SeckillResetService resetter,
                             ProductRepository products) {
        this.coordinator = coordinator;
        this.resetter = resetter;
        this.products = products;
    }

    /** 数据库中的商品列表。 */
    @GetMapping
    public List<ProductSnapshot> list() {
        return products.findAll();
    }

    /** 重置全部秒杀数据（清订单/幂等记录，库存恢复初始值，清 Redis 缓存）。 */
    @PostMapping("/reset")
    public SeckillResetResponse reset() {
        return resetter.resetAll();
    }

    /**
     * 秒杀下单。
     *
     * 必须带 JWT（Authorization: Bearer <token>）。
     * Header: Idempotency-Key — 幂等键，防止前端重试重复下单
     */
    @PostMapping("/{productId}/buy")
    public SeckillResponse buy(
            HttpServletRequest request,
            @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String productId,
            @RequestHeader("Idempotency-Key")
            @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String idempotencyKey) {
        // userId 由 JwtAuthFilter 从 JWT 里解析后注入 request attribute
        String userId = (String) request.getAttribute(JwtAuthFilter.USER_ID_ATTR);
        if (userId == null) {
            throw new IllegalStateException("JwtAuthFilter should have injected userId");
        }
        return coordinator.buy(productId, idempotencyKey, userId);
    }

    /** 查询商品实时状态（仅读，不加锁）。也需要 JWT 认证。 */
    @GetMapping("/{productId}")
    public ResponseEntity<ProductSnapshot> get(
            @PathVariable @Pattern(regexp = "[A-Za-z0-9_-]{1,64}") String productId) {
        return ResponseEntity.of(products.find(productId));
    }
}
