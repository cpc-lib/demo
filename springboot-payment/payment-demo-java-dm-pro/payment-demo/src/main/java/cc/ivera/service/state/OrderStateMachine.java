package cc.ivera.service.state;

import cc.ivera.enums.OrderStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 订单状态机 — 管理所有订单状态及转换规则。
 *
 * 替代散落在各 Service 中的状态判断逻辑。
 */
public class OrderStateMachine {

    private static final Map<String, OrderState> STATES = new ConcurrentHashMap<>();

    static {
        register(new NotPaidState());
        register(new PaidSuccessState());
        register(new ClosedState());
        register(new CanceledState());
        register(new RefundProcessingState());
        register(new PartialRefundState());
        register(new FullRefundState());
        register(new RefundAbnormalState());
    }

    private static void register(OrderState state) {
        STATES.put(state.getStateValue(), state);
    }

    public static OrderState of(String stateValue) {
        OrderState state = STATES.get(stateValue);
        if (state == null) {
            throw new IllegalArgumentException("未知的订单状态: " + stateValue);
        }
        return state;
    }

    // ---- 具体状态类 ----

    static class NotPaidState implements OrderState {
        @Override public String getStateValue() { return OrderStatus.NOTPAY.getType(); }
        @Override public boolean canPay() { return true; }
        @Override public boolean canCancel() { return true; }
        @Override public boolean canClose() { return true; }
        @Override public OrderState onPaySuccess() { return of(OrderStatus.SUCCESS.getType()); }
        @Override public OrderState onCancel() { return of(OrderStatus.CANCEL.getType()); }
        @Override public OrderState onClose() { return of(OrderStatus.CLOSED.getType()); }
    }

    static class PaidSuccessState implements OrderState {
        @Override public String getStateValue() { return OrderStatus.SUCCESS.getType(); }
        @Override public boolean canRefund() { return true; }
        @Override public OrderState onRefundProcessing() { return of(OrderStatus.REFUND_PROCESSING.getType()); }
        @Override public OrderState onPartialRefund() { return of(OrderStatus.PARTIAL_REFUND.getType()); }
        @Override public OrderState onFullRefund() { return of(OrderStatus.REFUND_SUCCESS.getType()); }
    }

    static class ClosedState implements OrderState {
        @Override public String getStateValue() { return OrderStatus.CLOSED.getType(); }
    }

    static class CanceledState implements OrderState {
        @Override public String getStateValue() { return OrderStatus.CANCEL.getType(); }
    }

    static class RefundProcessingState implements OrderState {
        @Override public String getStateValue() { return OrderStatus.REFUND_PROCESSING.getType(); }
        @Override public boolean canRefund() { return true; }
        @Override public OrderState onPartialRefund() { return of(OrderStatus.PARTIAL_REFUND.getType()); }
        @Override public OrderState onFullRefund() { return of(OrderStatus.REFUND_SUCCESS.getType()); }
        @Override public OrderState onRefundAbnormal() { return of(OrderStatus.REFUND_ABNORMAL.getType()); }
    }

    static class PartialRefundState implements OrderState {
        @Override public String getStateValue() { return OrderStatus.PARTIAL_REFUND.getType(); }
        @Override public boolean canRefund() { return true; }
        @Override public OrderState onFullRefund() { return of(OrderStatus.REFUND_SUCCESS.getType()); }
        @Override public OrderState onRefundAbnormal() { return of(OrderStatus.REFUND_ABNORMAL.getType()); }
    }

    static class FullRefundState implements OrderState {
        @Override public String getStateValue() { return OrderStatus.REFUND_SUCCESS.getType(); }
    }

    static class RefundAbnormalState implements OrderState {
        @Override public String getStateValue() { return OrderStatus.REFUND_ABNORMAL.getType(); }
        @Override public boolean canRefund() { return true; }
        @Override public OrderState onRefundProcessing() { return of(OrderStatus.REFUND_PROCESSING.getType()); }
        @Override public OrderState onPartialRefund() { return of(OrderStatus.PARTIAL_REFUND.getType()); }
        @Override public OrderState onFullRefund() { return of(OrderStatus.REFUND_SUCCESS.getType()); }
    }
}
