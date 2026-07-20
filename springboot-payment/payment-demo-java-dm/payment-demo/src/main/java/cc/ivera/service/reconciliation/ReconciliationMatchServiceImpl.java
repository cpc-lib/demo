package cc.ivera.service.reconciliation;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentInfo;
import cc.ivera.entity.reconciliation.ReconciliationBatch;
import cc.ivera.entity.reconciliation.ReconciliationDetail;
import cc.ivera.entity.reconciliation.ReconciliationDiscrepancy;
import cc.ivera.enums.reconciliation.DiscrepancyStatus;
import cc.ivera.enums.reconciliation.DiscrepancyType;
import cc.ivera.enums.reconciliation.MatchStatus;
import cc.ivera.exception.BizException;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.PaymentInfoMapper;
import cc.ivera.mapper.reconciliation.ReconciliationBatchMapper;
import cc.ivera.mapper.reconciliation.ReconciliationDetailMapper;
import cc.ivera.mapper.reconciliation.ReconciliationDiscrepancyMapper;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReconciliationMatchServiceImpl implements ReconciliationMatchService {

    private static final ZoneId ZONE_ID = ZoneId.of("Asia/Shanghai");

    private final ReconciliationBatchMapper batchMapper;

    private final ReconciliationDetailMapper detailMapper;

    private final ReconciliationDiscrepancyMapper discrepancyMapper;

    private final OrderInfoMapper orderInfoMapper;

    private final PaymentInfoMapper paymentInfoMapper;

    public ReconciliationMatchServiceImpl(
        ReconciliationBatchMapper batchMapper,
        ReconciliationDetailMapper detailMapper,
        ReconciliationDiscrepancyMapper discrepancyMapper,
        OrderInfoMapper orderInfoMapper,
        PaymentInfoMapper paymentInfoMapper
    ) {
        this.batchMapper = batchMapper;
        this.detailMapper = detailMapper;
        this.discrepancyMapper = discrepancyMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.paymentInfoMapper = paymentInfoMapper;
    }

    @Override
    public List<ReconciliationDetail> collectLocalTransactions(String channelCode, Long paymentAppId, String billDate) {
        log.info("采集本地交易数据，channelCode={}, paymentAppId={}, billDate={}", channelCode, paymentAppId, billDate);

        LocalDate date = LocalDate.parse(billDate);
        Date startTime = Date.from(date.atStartOfDay(ZONE_ID).toInstant());
        Date endTime = Date.from(date.atTime(LocalTime.MAX).atZone(ZONE_ID).toInstant());

        QueryWrapper<PaymentInfo> paymentQuery = new QueryWrapper<>();
        paymentQuery.eq("payment_type", channelCode)
                .ge("create_time", startTime)
                .le("create_time", endTime)
                .orderByAsc("create_time");

        List<PaymentInfo> paymentInfos = paymentInfoMapper.selectList(paymentQuery);
        log.info("查询到本地支付记录 {} 条", paymentInfos.size());

        List<ReconciliationDetail> result = new ArrayList<>();
        for (PaymentInfo paymentInfo : paymentInfos) {
            OrderInfo orderInfo = orderInfoMapper.selectOne(
                    new QueryWrapper<OrderInfo>().eq("order_no", paymentInfo.getOrderNo()));

            ReconciliationDetail detail = new ReconciliationDetail();
            detail.setOrderNo(paymentInfo.getOrderNo());
            detail.setTransactionId(paymentInfo.getTransactionId());
            detail.setTradeType("支付");
            detail.setLocalAmount(paymentInfo.getPayerTotal());
            detail.setLocalStatus(orderInfo == null ? null : orderInfo.getOrderStatus());
            detail.setTradeTime(paymentInfo.getCreateTime());
            result.add(detail);
        }

        return result;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void executeMatch(String batchNo) {
        log.info("开始执行对账匹配，batchNo={}", batchNo);

        ReconciliationBatch batch = batchMapper.selectByBatchNo(batchNo);
        if (batch == null) {
            throw new BizException("对账批次不存在，batchNo=" + batchNo);
        }

        List<ReconciliationDetail> channelDetails = detailMapper.selectByBatchNo(batchNo);
        List<ReconciliationDetail> localDetails = collectLocalTransactions(
                batch.getChannelCode(), batch.getPaymentAppId(), batch.getBillDate());

        Map<String, ReconciliationDetail> channelMap = channelDetails.stream()
                .collect(Collectors.toMap(ReconciliationDetail::getOrderNo, d -> d, (a, b) -> a));
        Map<String, ReconciliationDetail> localMap = localDetails.stream()
                .collect(Collectors.toMap(ReconciliationDetail::getOrderNo, d -> d, (a, b) -> a));

        Set<String> allOrderNos = new HashSet<>();
        allOrderNos.addAll(channelMap.keySet());
        allOrderNos.addAll(localMap.keySet());

        List<ReconciliationDetail> matchedDetails = new ArrayList<>();

        int matchedCount = 0;
        int matchedAmount = 0;
        int overpaymentCount = 0;
        int underpaymentCount = 0;
        int amountMismatchCount = 0;
        int statusMismatchCount = 0;

        for (String orderNo : allOrderNos) {
            ReconciliationDetail channelDetail = channelMap.get(orderNo);
            ReconciliationDetail localDetail = localMap.get(orderNo);

            ReconciliationDetail result = buildResultDetail(batchNo, channelDetail, localDetail);

            if (channelDetail != null && localDetail != null) {
                boolean amountMatch = Objects.equals(channelDetail.getChannelAmount(), localDetail.getLocalAmount());
                boolean statusMatch = Objects.equals(channelDetail.getChannelStatus(), localDetail.getLocalStatus())
                        || isStatusEquivalent(channelDetail.getChannelStatus(), localDetail.getLocalStatus());

                if (amountMatch && statusMatch) {
                    result.setMatchStatus(MatchStatus.MATCHED.name());
                    matchedCount++;
                    matchedAmount += localDetail.getLocalAmount() != null ? localDetail.getLocalAmount() : 0;
                } else if (!amountMatch) {
                    result.setMatchStatus(MatchStatus.MISMATCH.name());
                    result.setDiscrepancyType(DiscrepancyType.AMOUNT_MISMATCH.name());
                    amountMismatchCount++;
                } else {
                    result.setMatchStatus(MatchStatus.MISMATCH.name());
                    result.setDiscrepancyType(DiscrepancyType.STATUS_MISMATCH.name());
                    statusMismatchCount++;
                }
            } else if (channelDetail != null) {
                result.setMatchStatus(MatchStatus.CHANNEL_ONLY.name());
                result.setDiscrepancyType(DiscrepancyType.OVERPAYMENT.name());
                overpaymentCount++;
            } else {
                result.setMatchStatus(MatchStatus.LOCAL_ONLY.name());
                result.setDiscrepancyType(DiscrepancyType.UNDERPAYMENT.name());
                underpaymentCount++;
            }

            matchedDetails.add(result);
        }

        detailMapper.deleteByBatchNo(batchNo);
        if (!matchedDetails.isEmpty()) {
            detailMapper.batchInsert(matchedDetails);
        }

        List<ReconciliationDiscrepancy> finalDiscrepancies = new ArrayList<>();
        for (ReconciliationDetail detail : matchedDetails) {
            if (detail.getMatchStatus() != null && !MatchStatus.MATCHED.name().equals(detail.getMatchStatus())) {
                ReconciliationDiscrepancy discrepancy = new ReconciliationDiscrepancy();
                discrepancy.setBatchNo(batchNo);
                discrepancy.setDetailId(detail.getId());
                discrepancy.setDiscrepancyType(detail.getDiscrepancyType());
                discrepancy.setStatus(DiscrepancyStatus.OPEN.name());
                finalDiscrepancies.add(discrepancy);
            }
        }

        for (ReconciliationDiscrepancy discrepancy : finalDiscrepancies) {
            discrepancyMapper.insert(discrepancy);
        }

        int channelTotalCount = channelDetails.size();
        int channelTotalAmount = channelDetails.stream()
                .filter(d -> d.getChannelAmount() != null)
                .mapToInt(ReconciliationDetail::getChannelAmount)
                .sum();
        int localTotalCount = localDetails.size();
        int localTotalAmount = localDetails.stream()
                .filter(d -> d.getLocalAmount() != null)
                .mapToInt(ReconciliationDetail::getLocalAmount)
                .sum();
        int discrepancyCount = finalDiscrepancies.size();

        batch.setChannelTotalCount(channelTotalCount);
        batch.setChannelTotalAmount(channelTotalAmount);
        batch.setLocalTotalCount(localTotalCount);
        batch.setLocalTotalAmount(localTotalAmount);
        batch.setMatchedCount(matchedCount);
        batch.setMatchedAmount(matchedAmount);
        batch.setDiscrepancyCount(discrepancyCount);
        batch.setOverpaymentCount(overpaymentCount);
        batch.setUnderpaymentCount(underpaymentCount);
        batch.setAmountMismatchCount(amountMismatchCount);
        batch.setStatusMismatchCount(statusMismatchCount);
        batchMapper.updateById(batch);

        log.info("对账匹配完成，batchNo={}, 渠道记录={}, 本地记录={}, 匹配成功={}, 差异数={}",
                batchNo, channelTotalCount, localTotalCount, matchedCount, discrepancyCount);
    }

    private ReconciliationDetail buildResultDetail(String batchNo,
                                                    ReconciliationDetail channelDetail,
                                                    ReconciliationDetail localDetail) {
        ReconciliationDetail result = new ReconciliationDetail();
        result.setBatchNo(batchNo);

        if (channelDetail != null) {
            result.setOrderNo(channelDetail.getOrderNo());
            result.setTransactionId(channelDetail.getTransactionId());
            result.setTradeType(channelDetail.getTradeType());
            result.setChannelAmount(channelDetail.getChannelAmount());
            result.setChannelStatus(channelDetail.getChannelStatus());
            result.setTradeTime(channelDetail.getTradeTime());
        }

        if (localDetail != null) {
            if (result.getOrderNo() == null) {
                result.setOrderNo(localDetail.getOrderNo());
            }
            if (result.getTransactionId() == null) {
                result.setTransactionId(localDetail.getTransactionId());
            }
            if (result.getTradeType() == null) {
                result.setTradeType(localDetail.getTradeType());
            }
            result.setLocalAmount(localDetail.getLocalAmount());
            result.setLocalStatus(localDetail.getLocalStatus());
            if (result.getTradeTime() == null) {
                result.setTradeTime(localDetail.getTradeTime());
            }
        }

        return result;
    }

    private boolean isStatusEquivalent(String channelStatus, String localStatus) {
        if (channelStatus == null || localStatus == null) {
            return false;
        }
        String ch = channelStatus.toUpperCase();
        String lo = localStatus.toUpperCase();
        return ch.contains("SUCCESS") && lo.contains("SUCCESS")
                || ch.contains("PAID") && lo.contains("SUCCESS")
                || ch.contains("TRADE_SUCCESS") && lo.contains("SUCCESS");
    }
}
