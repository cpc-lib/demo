package cc.ivera.cache.sentinel;

import cc.ivera.cache.entity.Order;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class OrderBlockHandlerTest {

    @Test
    void returnsFriendlyBlockedResponse() {
        Order order = OrderBlockHandler.handleGetOrderBlocked(1L, null);

        assertThat(order.getId()).isEqualTo(1L);
        assertThat(order.getName()).isEqualTo("访问过快，请稍后重试");
    }
}
