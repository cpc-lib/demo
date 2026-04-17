package cc.ivera.ordermachine.components;

import cc.ivera.ordermachine.domain.enums.OrderNoPrefix;
import cc.ivera.ordermachine.util.OrderNoGenerator;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Component
public class OrderNoGenerateHelper {

    private final OrderNoGenerator generator;

    public OrderNoGenerateHelper(@Value("${order.no.machine-id:1}") int machineId) {
        this.generator = new OrderNoGenerator(machineId);
    }

    /**
     * 普通订单号
     */
    public String nextOrderNo() {
        return generator.nextOrderNo(OrderNoPrefix.ORDER.getCode());
    }

    /**
     * 支付单号
     */
    public String nextPayNo() {
        return generator.nextOrderNo(OrderNoPrefix.PAY.getCode());
    }

    /**
     * 退款单号
     */
    public String nextRefundNo() {
        return generator.nextOrderNo(OrderNoPrefix.REFUND.getCode());
    }

    /**
     * 自定义前缀
     */
    public String nextNo(String prefix) {
        return generator.nextOrderNo(prefix);
    }
}