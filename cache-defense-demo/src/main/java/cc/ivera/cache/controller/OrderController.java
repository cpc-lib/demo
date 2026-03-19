
package cc.ivera.cache.controller;

import cc.ivera.cache.entity.Order;
import cc.ivera.cache.service.OrderService;
import cc.ivera.cache.sentinel.OrderBlockHandler;
import cc.ivera.cache.sentinel.OrderQueryRateLimitSupport;
import com.alibaba.csp.sentinel.Entry;
import com.alibaba.csp.sentinel.EntryType;
import com.alibaba.csp.sentinel.SphU;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;

    @GetMapping("/order/{id}")
    public Order get(@PathVariable Long id, HttpServletRequest request){
        String clientIp = OrderQueryRateLimitSupport.resolveClientIp(request);
        String limitKey = OrderQueryRateLimitSupport.buildLimitKey(clientIp, id);
        Entry entry = null;
        try {
            entry = SphU.entry(OrderBlockHandler.GET_ORDER_RESOURCE, EntryType.IN, 1, limitKey);
            return orderService.getById(id);
        } catch (BlockException ex) {
            return OrderBlockHandler.handleGetOrderBlocked(id, clientIp, limitKey, ex);
        } finally {
            if (entry != null) {
                entry.exit();
            }
        }
    }

    @PostMapping("/order")
    @ResponseStatus(HttpStatus.CREATED)
    public Order create(@RequestBody Order order) {
        return orderService.create(order);
    }
}
