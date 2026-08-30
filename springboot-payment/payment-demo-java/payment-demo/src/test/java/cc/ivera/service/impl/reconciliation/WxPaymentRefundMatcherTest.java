package cc.ivera.service.impl.reconciliation;

import cc.ivera.entity.PaymentInfo;
import cc.ivera.entity.ReconciliationDetail;
import cc.ivera.entity.RefundInfo;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("微信进账与退款逐笔匹配测试")
class WxPaymentRefundMatcherTest {

    private final WxPaymentRefundMatcher matcher = new WxPaymentRefundMatcher();

    @Test
    @DisplayName("进账与退款使用各自流水和标识匹配")
    void testMatchPaymentAndRefundIndependently() {
        List<ChannelBillRecord> channelRecords = Arrays.asList(
                channelPayment("ORDER_PAY_001", "WX_TX_001", 100, "SUCCESS"),
                channelRefund("ORDER_PAY_OLD", "WX_TX_OLD", "REFUND_001", "WX_REFUND_001", 60, "SUCCESS")
        );

        List<ReconciliationDetail> details = matcher.match(
                channelRecords,
                Collections.singletonList(localPayment("ORDER_PAY_001", "WX_TX_001", 100, "SUCCESS")),
                Collections.singletonList(localRefund("ORDER_PAY_OLD", "REFUND_001", "WX_REFUND_001", 60, "SUCCESS")),
                7L);

        assertThat(details).hasSize(2);
        assertThat(details).extracting(ReconciliationDetail::getBusinessType)
                .containsExactly("PAYMENT", "REFUND");
        assertThat(details).extracting(ReconciliationDetail::getDiffType)
                .containsOnly("MATCH");
        assertThat(details.get(1).getRefundNo()).isEqualTo("REFUND_001");
        assertThat(details.get(1).getRefundId()).isEqualTo("WX_REFUND_001");
    }

    @Test
    @DisplayName("渠道缺失、本地缺失、金额差异、状态差异均生成明细")
    void testBuildAllDifferenceTypes() {
        List<ChannelBillRecord> channelRecords = Arrays.asList(
                channelPayment("ORDER_AMOUNT", "WX_AMOUNT", 101, "SUCCESS"),
                channelPayment("ORDER_STATUS", "WX_STATUS", 100, "SUCCESS"),
                channelRefund("ORDER_CHANNEL_ONLY", "WX_OLD", "REFUND_CHANNEL_ONLY", "WX_REFUND_ONLY", 50, "SUCCESS")
        );
        List<PaymentInfo> payments = Arrays.asList(
                localPayment("ORDER_AMOUNT", "WX_AMOUNT", 100, "SUCCESS"),
                localPayment("ORDER_STATUS", "WX_STATUS", 100, "NOTPAY"),
                localPayment("ORDER_LOCAL_ONLY", "WX_LOCAL_ONLY", 80, "SUCCESS")
        );

        List<ReconciliationDetail> details = matcher.match(
                channelRecords, payments, Collections.emptyList(), 8L);

        assertThat(details).extracting(ReconciliationDetail::getDiffType)
                .containsExactlyInAnyOrder(
                        "AMOUNT_MISMATCH", "STATUS_MISMATCH", "MISSING_CHANNEL", "MISSING_LOCAL");
        ReconciliationDetail amountMismatch = details.stream()
                .filter(detail -> "AMOUNT_MISMATCH".equals(detail.getDiffType()))
                .findFirst().orElse(null);
        ReconciliationDetail missingLocal = details.stream()
                .filter(detail -> "MISSING_LOCAL".equals(detail.getDiffType()))
                .findFirst().orElse(null);
        assertThat(amountMismatch).isNotNull();
        assertThat(amountMismatch.getDiffAmount()).isEqualTo(1);
        assertThat(missingLocal).isNotNull();
        assertThat(missingLocal.getBusinessType()).isEqualTo("REFUND");
    }

    @Test
    @DisplayName("账单退款快照处理中而本地已成功视为状态兼容")
    void testRefundStatusAllowsLocalProgress() {
        ChannelBillRecord channel = channelRefund(
                "ORDER_REFUND_PROGRESS", "WX_OLD", "REFUND_PROGRESS", "WX_REFUND_PROGRESS", 30, "PROCESSING");
        RefundInfo local = localRefund(
                "ORDER_REFUND_PROGRESS", "REFUND_PROGRESS", "WX_REFUND_PROGRESS", 30, "SUCCESS");

        List<ReconciliationDetail> details = matcher.match(
                Collections.singletonList(channel), Collections.emptyList(), Collections.singletonList(local), 9L);

        assertThat(details).hasSize(1);
        assertThat(details.get(0).getDiffType()).isEqualTo("MATCH");
    }

    @Test
    @DisplayName("渠道仅有REFUND交易状态时，已审核的旧退款空状态视为兼容")
    void testRefundTradeStateAllowsApprovedLegacyRecordWithoutStatus() {
        ChannelBillRecord channel = channelRefund(
                "ORDER_REFUND_LEGACY", "WX_OLD", "REFUND_LEGACY", "WX_REFUND_LEGACY", 30, "REFUND");
        RefundInfo local = localRefund(
                "ORDER_REFUND_LEGACY", "REFUND_LEGACY", "WX_REFUND_LEGACY", 30, null);
        local.setApprovalStatus("APPROVED");

        List<ReconciliationDetail> details = matcher.match(
                Collections.singletonList(channel), Collections.emptyList(), Collections.singletonList(local), 13L);

        assertThat(details).hasSize(1);
        assertThat(details.get(0).getDiffType()).isEqualTo("MATCH");
    }

    @Test
    @DisplayName("相同微信订单号的不同商户订单不会互相覆盖")
    void testDuplicateTransactionIdDoesNotOverwriteRows() {
        List<ChannelBillRecord> channelRecords = Arrays.asList(
                channelPayment("ORDER_DUP_001", "WX_DUP", 100, "SUCCESS"),
                channelPayment("ORDER_DUP_002", "WX_DUP", 200, "SUCCESS")
        );
        List<PaymentInfo> payments = Arrays.asList(
                localPayment("ORDER_DUP_001", "WX_DUP", 100, "SUCCESS"),
                localPayment("ORDER_DUP_002", "WX_DUP", 200, "SUCCESS")
        );

        List<ReconciliationDetail> details = matcher.match(
                channelRecords, payments, Collections.emptyList(), 10L);

        assertThat(details).hasSize(2);
        assertThat(details).extracting(ReconciliationDetail::getDiffType).containsOnly("MATCH");
        assertThat(details).extracting(ReconciliationDetail::getOrderNo)
                .containsExactly("ORDER_DUP_001", "ORDER_DUP_002");
    }

    @Test
    @DisplayName("回退渠道号不唯一时不做猜测匹配")
    void testAmbiguousFallbackKeyRemainsDifferent() {
        ChannelBillRecord first = channelPayment(null, "WX_AMBIGUOUS", 100, "SUCCESS");
        ChannelBillRecord second = channelPayment(null, "WX_AMBIGUOUS", 100, "SUCCESS");
        PaymentInfo local = localPayment("ORDER_AMBIGUOUS", "WX_AMBIGUOUS", 100, "SUCCESS");

        List<ReconciliationDetail> details = matcher.match(
                Arrays.asList(first, second), Collections.singletonList(local), Collections.emptyList(), 11L);

        assertThat(details).hasSize(3);
        assertThat(details).extracting(ReconciliationDetail::getDiffType)
                .containsExactlyInAnyOrder("MISSING_CHANNEL", "MISSING_LOCAL", "MISSING_LOCAL");
    }

    @Test
    @DisplayName("唯一渠道号回退匹配时保留本地商户标识")
    void testUniqueFallbackKeepsLocalMerchantKeys() {
        ChannelBillRecord paymentChannel = channelPayment(null, "WX_FALLBACK", 100, "SUCCESS");
        ChannelBillRecord refundChannel = channelRefund(
                "ORDER_REFUND_FALLBACK", "WX_OLD", null, "WX_REFUND_FALLBACK", 30, "SUCCESS");
        PaymentInfo payment = localPayment("ORDER_FALLBACK", "WX_FALLBACK", 100, "SUCCESS");
        RefundInfo refund = localRefund(
                "ORDER_REFUND_FALLBACK", "REFUND_FALLBACK", "WX_REFUND_FALLBACK", 30, "SUCCESS");

        List<ReconciliationDetail> details = matcher.match(
                Arrays.asList(paymentChannel, refundChannel),
                Collections.singletonList(payment),
                Collections.singletonList(refund), 12L);

        assertThat(details).extracting(ReconciliationDetail::getDiffType).containsOnly("MATCH");
        assertThat(details.get(0).getOrderNo()).isEqualTo("ORDER_FALLBACK");
        assertThat(details.get(1).getRefundNo()).isEqualTo("REFUND_FALLBACK");
    }

    private ChannelBillRecord channelPayment(String orderNo, String transactionId, int amount, String status) {
        ChannelBillRecord record = new ChannelBillRecord();
        record.setBusinessType("PAYMENT");
        record.setOrderNo(orderNo);
        record.setTransactionId(transactionId);
        record.setAmount(amount);
        record.setStatus(status);
        record.setTradeTime(new Date(1000L));
        return record;
    }

    private ChannelBillRecord channelRefund(String orderNo, String transactionId,
                                             String refundNo, String refundId,
                                             int amount, String status) {
        ChannelBillRecord record = new ChannelBillRecord();
        record.setBusinessType("REFUND");
        record.setOrderNo(orderNo);
        record.setTransactionId(transactionId);
        record.setRefundNo(refundNo);
        record.setRefundId(refundId);
        record.setAmount(amount);
        record.setRefundAmount(amount);
        record.setStatus(status);
        record.setTradeTime(new Date(2000L));
        return record;
    }

    private PaymentInfo localPayment(String orderNo, String transactionId, int amount, String status) {
        PaymentInfo payment = new PaymentInfo();
        payment.setOrderNo(orderNo);
        payment.setTransactionId(transactionId);
        payment.setPayerTotal(amount);
        payment.setTradeState(status);
        payment.setCreateTime(new Date(3000L));
        return payment;
    }

    private RefundInfo localRefund(String orderNo, String refundNo, String refundId, int amount, String status) {
        RefundInfo refund = new RefundInfo();
        refund.setOrderNo(orderNo);
        refund.setRefundNo(refundNo);
        refund.setRefundId(refundId);
        refund.setRefund(amount);
        refund.setRefundStatus(status);
        refund.setApprovedTime(new Date(4000L));
        refund.setCreateTime(new Date(3500L));
        return refund;
    }
}
