package cc.ivera.service.state;

/**
 * 退款状态接口 — State 模式。
 */
public interface RefundState {

    /** 状态标识值（与 RefundStatus 枚举值一致） */
    String getStateValue();

    /** 是否可以提交到渠道 */
    default boolean canSubmitToChannel() { return false; }

    /** 是否可以同步状态 */
    default boolean canSyncStatus() { return false; }

    /** 是否为终态 */
    default boolean isTerminal() { return false; }

    /** 提交到渠道后的目标状态 */
    default RefundState onSubmittedToChannel() { return this; }

    /** 退款成功 */
    default RefundState onSuccess() { return this; }

    /** 退款失败 */
    default RefundState onFailed() { return this; }

    /** 退款关闭 */
    default RefundState onClosed() { return this; }

    /** 退款异常 */
    default RefundState onAbnormal() { return this; }
}
