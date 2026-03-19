package cc.ivera.cache.sentinel;

import cc.ivera.cache.entity.Order;
import com.alibaba.csp.sentinel.slots.block.BlockException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class OrderBlockHandler {

    public static final String GET_ORDER_RESOURCE = "getOrder";

    private static final Logger log = LoggerFactory.getLogger(OrderBlockHandler.class);

    private OrderBlockHandler() {
    }

    public static Order handleGetOrderBlocked(Long id, BlockException ex) {
        return handleGetOrderBlocked(id, null, null, ex);
    }

    public static Order handleGetOrderBlocked(Long id, String clientIp, String limitKey, BlockException ex) {
        log.warn("Sentinel blocked order query, clientIp={}, id={}, limitKey={}", clientIp, id, limitKey, ex);

        Order order = new Order();
        order.setId(id);
        order.setName("访问过快，请稍后重试");
        return order;
    }
}
