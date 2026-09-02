package cc.ivera.controller;

import cc.ivera.dto.OrderIdempotencyKeyView;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.OrderIdempotencyService;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Api(tags = "订单幂等键")
@RestController
@RequestMapping("/api/order-info/idempotency-keys")
public class OrderIdempotencyController {

    private final OrderIdempotencyService service;

    public OrderIdempotencyController(OrderIdempotencyService service) {
        this.service = service;
    }

    @ApiOperation("签发一次性下单幂等键")
    @PostMapping
    public R<OrderIdempotencyKeyView> issue() {
        AuthUser user = AuthContext.requireShoppingUser();
        return R.ok(service.issue(user));
    }
}
