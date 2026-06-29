package cc.ivera.service.state;

/**
 * 订单状态接口 — State 模式。
 *
 * 将散落在各 Service 中的状态转换逻辑集中到状态类中。
 * 每个状态实现类封装该状态下的合法操作和转换规则。
 */
public interface OrderState {

    /** 状态标识值（与 OrderStatus 枚举值一致） */
    String getStateValue();

    /** 是否可以支付 */
    default boolean canPay() { return false; }

    /** 是否可以取消 */
    default boolean canCancel() { return false; }

    /** 是否可以退款 */
    default boolean canRefund() { return false; }

    /** 是否可以关闭 */
    default boolean canClose() { return false; }

    /** 支付成功后的目标状态 */
    default OrderState onPaySuccess() { return this; }

    /** 取消后的目标状态 */
    default OrderState onCancel() { return this; }

    /** 关闭后的目标状态 */
    default OrderState onClose() { return this; }

    /** 退款处理中 */
    default OrderState onRefundProcessing() { return this; }

    /** 部分退款 */
    default OrderState onPartialRefund() { return this; }

    /** 全额退款 */
    default OrderState onFullRefund() { return this; }

    /** 退款异常 */
    default OrderState onRefundAbnormal() { return this; }
}
