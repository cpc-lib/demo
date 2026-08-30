package cc.ivera.service.impl.reconciliation;

import cc.ivera.entity.PaymentInfo;
import cc.ivera.entity.ReconciliationDetail;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.DiffType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.function.Function;

/**
 * 微信交易账单只对两类独立业务流水做逐笔匹配：PAYMENT 与 REFUND。
 */
@Component
public class WxPaymentRefundMatcher {

    private static final String PAYMENT = "PAYMENT";
    private static final String REFUND = "REFUND";

    public List<ReconciliationDetail> match(List<ChannelBillRecord> channelRecords,
                                             List<PaymentInfo> payments,
                                             List<RefundInfo> refunds,
                                             Long reconciliationId) {
        List<ChannelBillRecord> channels = channelRecords == null
                ? Collections.emptyList() : channelRecords;
        List<PaymentInfo> localPayments = payments == null
                ? Collections.emptyList() : payments;
        List<RefundInfo> localRefunds = refunds == null
                ? Collections.emptyList() : refunds;

        List<ChannelBillRecord> paymentChannels = filterByBusinessType(channels, PAYMENT);
        List<ChannelBillRecord> refundChannels = filterByBusinessType(channels, REFUND);

        Map<String, List<ChannelBillRecord>> paymentsByOrderNo = index(paymentChannels, ChannelBillRecord::getOrderNo);
        Map<String, List<ChannelBillRecord>> paymentsByTransactionId = index(paymentChannels, ChannelBillRecord::getTransactionId);
        Map<String, List<ChannelBillRecord>> refundsByRefundNo = index(refundChannels, ChannelBillRecord::getRefundNo);
        Map<String, List<ChannelBillRecord>> refundsByRefundId = index(refundChannels, ChannelBillRecord::getRefundId);

        Set<ChannelBillRecord> matchedChannels = Collections.newSetFromMap(new IdentityHashMap<>());
        List<ReconciliationDetail> details = new ArrayList<>();

        for (PaymentInfo payment : localPayments) {
            ChannelBillRecord channel = findFirstUnmatched(
                    paymentsByOrderNo.get(payment.getOrderNo()), matchedChannels);
            if (channel == null) {
                channel = findUniqueUnmatched(
                        paymentsByTransactionId.get(payment.getTransactionId()), matchedChannels);
            }
            if (channel != null) {
                matchedChannels.add(channel);
            }
            details.add(buildPaymentDetail(channel, payment, reconciliationId));
        }

        for (RefundInfo refund : localRefunds) {
            ChannelBillRecord channel = findFirstUnmatched(
                    refundsByRefundNo.get(refund.getRefundNo()), matchedChannels);
            if (channel == null) {
                channel = findUniqueUnmatched(
                        refundsByRefundId.get(refund.getRefundId()), matchedChannels);
            }
            if (channel != null) {
                matchedChannels.add(channel);
            }
            details.add(buildRefundDetail(channel, refund, reconciliationId));
        }

        for (ChannelBillRecord channel : channels) {
            if ((PAYMENT.equals(channel.getBusinessType()) || REFUND.equals(channel.getBusinessType()))
                    && !matchedChannels.contains(channel)) {
                details.add(buildMissingLocalDetail(channel, reconciliationId));
            }
        }
        return details;
    }

    private ReconciliationDetail buildPaymentDetail(ChannelBillRecord channel,
                                                      PaymentInfo local,
                                                      Long reconciliationId) {
        ReconciliationDetail detail = baseDetail(reconciliationId, PAYMENT);
        detail.setOrderNo(local.getOrderNo());
        detail.setTransactionId(local.getTransactionId());
        detail.setLocalAmount(local.getPayerTotal());
        detail.setLocalStatus(local.getTradeState());
        detail.setLocalTradeTime(local.getCreateTime());

        if (channel == null) {
            detail.setDiffType(DiffType.MISSING_CHANNEL.name());
            detail.setRemark("本地有微信进账流水但渠道账单无记录");
            return detail;
        }

        applyChannel(detail, channel);
        if (!StringUtils.hasText(detail.getOrderNo())) {
            detail.setOrderNo(channel.getOrderNo());
        }
        if (!StringUtils.hasText(detail.getTransactionId())) {
            detail.setTransactionId(channel.getTransactionId());
        }
        applyComparison(detail, paymentStatusMatches(channel.getStatus(), local.getTradeState()));
        return detail;
    }

    private ReconciliationDetail buildRefundDetail(ChannelBillRecord channel,
                                                     RefundInfo local,
                                                     Long reconciliationId) {
        ReconciliationDetail detail = baseDetail(reconciliationId, REFUND);
        detail.setOrderNo(local.getOrderNo());
        detail.setRefundNo(local.getRefundNo());
        detail.setRefundId(local.getRefundId());
        detail.setLocalAmount(local.getRefund());
        detail.setLocalStatus(local.getRefundStatus());
        detail.setLocalTradeTime(local.getApprovedTime() == null ? local.getCreateTime() : local.getApprovedTime());

        if (channel == null) {
            detail.setDiffType(DiffType.MISSING_CHANNEL.name());
            detail.setRemark("本地有微信退款流水但渠道账单无记录");
            return detail;
        }

        applyChannel(detail, channel);
        detail.setTransactionId(channel.getTransactionId());
        if (!StringUtils.hasText(detail.getOrderNo())) {
            detail.setOrderNo(channel.getOrderNo());
        }
        if (!StringUtils.hasText(detail.getRefundNo())) {
            detail.setRefundNo(channel.getRefundNo());
        }
        if (!StringUtils.hasText(detail.getRefundId())) {
            detail.setRefundId(channel.getRefundId());
        }
        applyComparison(detail, refundStatusMatches(
                channel.getStatus(), local.getRefundStatus(), local.getApprovalStatus()));
        return detail;
    }

    private ReconciliationDetail buildMissingLocalDetail(ChannelBillRecord channel, Long reconciliationId) {
        ReconciliationDetail detail = baseDetail(reconciliationId, channel.getBusinessType());
        applyChannel(detail, channel);
        detail.setOrderNo(channel.getOrderNo());
        detail.setTransactionId(channel.getTransactionId());
        detail.setRefundNo(channel.getRefundNo());
        detail.setRefundId(channel.getRefundId());
        detail.setDiffType(DiffType.MISSING_LOCAL.name());
        detail.setRemark(PAYMENT.equals(channel.getBusinessType())
                ? "渠道账单有微信进账但本地无对应支付流水"
                : "渠道账单有微信退款但本地无对应退款流水");
        return detail;
    }

    private ReconciliationDetail baseDetail(Long reconciliationId, String businessType) {
        ReconciliationDetail detail = new ReconciliationDetail();
        detail.setReconciliationId(reconciliationId);
        detail.setBusinessType(businessType);
        return detail;
    }

    private void applyChannel(ReconciliationDetail detail, ChannelBillRecord channel) {
        detail.setChannelAmount(channel.getAmount());
        detail.setChannelStatus(channel.getStatus());
        detail.setChannelTradeTime(channel.getTradeTime());
    }

    private void applyComparison(ReconciliationDetail detail, boolean statusMatches) {
        boolean amountMatches = Objects.equals(detail.getChannelAmount(), detail.getLocalAmount());
        if (amountMatches && statusMatches) {
            detail.setDiffType(DiffType.MATCH.name());
            return;
        }
        if (!amountMatches) {
            detail.setDiffType(DiffType.AMOUNT_MISMATCH.name());
            detail.setDiffAmount(Math.abs(valueOrZero(detail.getChannelAmount()) - valueOrZero(detail.getLocalAmount())));
            detail.setRemark(statusMatches ? "金额不匹配" : "金额和状态均不匹配");
            return;
        }
        detail.setDiffType(DiffType.STATUS_MISMATCH.name());
        detail.setRemark("状态不匹配，渠道:" + detail.getChannelStatus() + "，本地:" + detail.getLocalStatus());
    }

    private int valueOrZero(Integer value) {
        return value == null ? 0 : value;
    }

    private boolean paymentStatusMatches(String channelStatus, String localStatus) {
        return isPaymentSuccess(channelStatus) && isPaymentSuccess(localStatus);
    }

    private boolean isPaymentSuccess(String status) {
        if (!StringUtils.hasText(status)) {
            return false;
        }
        return "SUCCESS".equalsIgnoreCase(status)
                || "TRADE_SUCCESS".equalsIgnoreCase(status)
                || "支付成功".equals(status)
                || "交易成功".equals(status);
    }

    private boolean refundStatusMatches(String channelStatus, String localStatus, String approvalStatus) {
        String channel = normalizeRefundStatus(channelStatus);
        String local = normalizeRefundStatus(localStatus);
        if (!StringUtils.hasText(channel)) {
            return false;
        }
        if ("REFUND".equals(channel)) {
            if (!StringUtils.hasText(local)) {
                return "APPROVED".equalsIgnoreCase(approvalStatus);
            }
            return !"FAILED".equals(local) && !"CLOSED".equals(local);
        }
        if (!StringUtils.hasText(local)) {
            return false;
        }
        if (channel.equals(local)) {
            return true;
        }
        if (("PROCESSING".equals(channel) || "ABNORMAL".equals(channel)) && "SUCCESS".equals(local)) {
            return true;
        }
        return false;
    }

    private String normalizeRefundStatus(String status) {
        if (!StringUtils.hasText(status)) {
            return "";
        }
        if ("退款成功".equals(status)) {
            return "SUCCESS";
        }
        if ("退款处理中".equals(status)) {
            return "PROCESSING";
        }
        if ("退款异常".equals(status)) {
            return "ABNORMAL";
        }
        if ("退款关闭".equals(status)) {
            return "CLOSED";
        }
        return status.trim().toUpperCase();
    }

    private List<ChannelBillRecord> filterByBusinessType(List<ChannelBillRecord> records, String businessType) {
        List<ChannelBillRecord> filtered = new ArrayList<>();
        for (ChannelBillRecord record : records) {
            if (record != null && businessType.equals(record.getBusinessType())) {
                filtered.add(record);
            }
        }
        return filtered;
    }

    private Map<String, List<ChannelBillRecord>> index(List<ChannelBillRecord> records,
                                                        Function<ChannelBillRecord, String> keyGetter) {
        Map<String, List<ChannelBillRecord>> index = new HashMap<>();
        for (ChannelBillRecord record : records) {
            String key = keyGetter.apply(record);
            if (StringUtils.hasText(key)) {
                index.computeIfAbsent(key, ignored -> new ArrayList<>()).add(record);
            }
        }
        return index;
    }

    private ChannelBillRecord findFirstUnmatched(List<ChannelBillRecord> candidates,
                                                   Set<ChannelBillRecord> matched) {
        if (candidates == null) {
            return null;
        }
        for (ChannelBillRecord candidate : candidates) {
            if (!matched.contains(candidate)) {
                return candidate;
            }
        }
        return null;
    }

    private ChannelBillRecord findUniqueUnmatched(List<ChannelBillRecord> candidates,
                                                    Set<ChannelBillRecord> matched) {
        if (candidates == null || candidates.size() != 1 || matched.contains(candidates.get(0))) {
            return null;
        }
        return candidates.get(0);
    }
}
