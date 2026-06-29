package cc.ivera.service.state;

import cc.ivera.enums.RefundStatus;

import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * 退款状态机 — 管理所有退款状态及转换规则。
 */
public class RefundStateMachine {

    private static final Map<String, RefundState> STATES = new ConcurrentHashMap<>();

    static {
        register(new CreatedRefundState());
        register(new ProcessingRefundState());
        register(new SuccessRefundState());
        register(new FailedRefundState());
        register(new ClosedRefundState());
        register(new AbnormalRefundState());
    }

    private static void register(RefundState state) {
        STATES.put(state.getStateValue(), state);
    }

    public static RefundState of(String stateValue) {
        RefundState state = STATES.get(stateValue);
        if (state == null) {
            throw new IllegalArgumentException("未知的退款状态: " + stateValue);
        }
        return state;
    }

    // ---- 具体状态类 ----

    static class CreatedRefundState implements RefundState {
        @Override public String getStateValue() { return RefundStatus.CREATED.getType(); }
        @Override public boolean canSubmitToChannel() { return true; }
        @Override public RefundState onSubmittedToChannel() { return of(RefundStatus.PROCESSING.getType()); }
    }

    static class ProcessingRefundState implements RefundState {
        @Override public String getStateValue() { return RefundStatus.PROCESSING.getType(); }
        @Override public boolean canSyncStatus() { return true; }
        @Override public RefundState onSuccess() { return of(RefundStatus.SUCCESS.getType()); }
        @Override public RefundState onFailed() { return of(RefundStatus.FAILED.getType()); }
        @Override public RefundState onAbnormal() { return of(RefundStatus.ABNORMAL.getType()); }
    }

    static class SuccessRefundState implements RefundState {
        @Override public String getStateValue() { return RefundStatus.SUCCESS.getType(); }
        @Override public boolean isTerminal() { return true; }
    }

    static class FailedRefundState implements RefundState {
        @Override public String getStateValue() { return RefundStatus.FAILED.getType(); }
        @Override public boolean isTerminal() { return true; }
    }

    static class ClosedRefundState implements RefundState {
        @Override public String getStateValue() { return RefundStatus.CLOSED.getType(); }
        @Override public boolean isTerminal() { return true; }
    }

    static class AbnormalRefundState implements RefundState {
        @Override public String getStateValue() { return RefundStatus.ABNORMAL.getType(); }
        @Override public boolean canSyncStatus() { return true; }
        @Override public RefundState onSuccess() { return of(RefundStatus.SUCCESS.getType()); }
        @Override public RefundState onFailed() { return of(RefundStatus.FAILED.getType()); }
        @Override public RefundState onClosed() { return of(RefundStatus.CLOSED.getType()); }
    }
}
