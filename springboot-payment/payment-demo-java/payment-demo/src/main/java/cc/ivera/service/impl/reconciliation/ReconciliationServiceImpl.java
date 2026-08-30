package cc.ivera.service.impl.reconciliation;

import cc.ivera.dto.ReconciliationExecuteRequest;
import cc.ivera.entity.ChannelBill;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentInfo;
import cc.ivera.entity.Reconciliation;
import cc.ivera.entity.ReconciliationDetail;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.DiffType;
import cc.ivera.enums.PayType;
import cc.ivera.enums.ReconciliationStatus;
import cc.ivera.enums.RefundApprovalStatus;
import cc.ivera.exception.BizException;
import cc.ivera.mapper.ChannelBillMapper;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.PaymentInfoMapper;
import cc.ivera.mapper.ReconciliationDetailMapper;
import cc.ivera.mapper.ReconciliationMapper;
import cc.ivera.mapper.RefundInfoMapper;
import cc.ivera.service.reconciliation.ReconciliationService;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.io.ByteArrayOutputStream;
import java.io.OutputStreamWriter;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.format.DateTimeParseException;
import java.util.*;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Service
@Slf4j
public class ReconciliationServiceImpl implements ReconciliationService {

    private static final long RECON_LOCK_WAIT_SECONDS = 5;
    private static final long RECON_LOCK_LEASE_SECONDS = 1800;
    private static final String RECON_LOCK_PREFIX = "recon:lock:";
    private static final String CHANNEL_WXPAY = "WXPAY";
    private static final String CHANNEL_ALIPAY = "ALIPAY";
    private static final String BILL_TYPE_ALL = "ALL";

    private final ReconciliationMapper reconciliationMapper;
    private final ReconciliationDetailMapper reconciliationDetailMapper;
    private final ChannelBillMapper channelBillMapper;
    private final OrderInfoMapper orderInfoMapper;
    private final PaymentInfoMapper paymentInfoMapper;
    private final RefundInfoMapper refundInfoMapper;
    private final WxPaymentRefundMatcher wxPaymentRefundMatcher;
    private final WxBillParser wxBillParser;
    private final AliPayBillParser aliPayBillParser;
    private final StringRedisTemplate stringRedisTemplate;

    public ReconciliationServiceImpl(
        ReconciliationMapper reconciliationMapper,
        ReconciliationDetailMapper reconciliationDetailMapper,
        ChannelBillMapper channelBillMapper,
        OrderInfoMapper orderInfoMapper,
        PaymentInfoMapper paymentInfoMapper,
        RefundInfoMapper refundInfoMapper,
        WxPaymentRefundMatcher wxPaymentRefundMatcher,
        WxBillParser wxBillParser,
        AliPayBillParser aliPayBillParser,
        StringRedisTemplate stringRedisTemplate
    ) {
        this.reconciliationMapper = reconciliationMapper;
        this.reconciliationDetailMapper = reconciliationDetailMapper;
        this.channelBillMapper = channelBillMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.paymentInfoMapper = paymentInfoMapper;
        this.refundInfoMapper = refundInfoMapper;
        this.wxPaymentRefundMatcher = wxPaymentRefundMatcher;
        this.wxBillParser = wxBillParser;
        this.aliPayBillParser = aliPayBillParser;
        this.stringRedisTemplate = stringRedisTemplate;
    }

    @Override
    @Transactional
    public Reconciliation executeReconciliation(ReconciliationExecuteRequest request) {
        LocalDate billDate = parseBillDate(request.getBillDate());
        String channelCode = request.getChannelCode();
        Long paymentAppId = request.getPaymentAppId();
        String billType = StringUtils.hasText(request.getBillType()) ? request.getBillType() : BILL_TYPE_ALL;
        boolean force = Boolean.TRUE.equals(request.getForce());

        validateChannelCode(channelCode);

        String lockKey = buildLockKey(billDate, channelCode, paymentAppId);
        Boolean lockAcquired = stringRedisTemplate.opsForValue()
                .setIfAbsent(lockKey, "locked", RECON_LOCK_LEASE_SECONDS, TimeUnit.SECONDS);
        if (lockAcquired == null || !lockAcquired) {
            Reconciliation existing = reconciliationMapper.selectByUniqueKey(billDate, channelCode, paymentAppId, billType);
            if (existing != null) {
                return existing;
            }
            throw new BizException("对账任务正在执行中，请稍后再试");
        }

        try {
            Reconciliation existing = reconciliationMapper.selectByUniqueKey(billDate, channelCode, paymentAppId, billType);
            if (existing != null && !force && ReconciliationStatus.COMPLETED.name().equals(existing.getStatus())) {
                log.info("对账记录已存在，直接返回，billDate={}, channelCode={}", billDate, channelCode);
                return existing;
            }

            Reconciliation reconciliation;
            if (existing != null) {
                reconciliation = existing;
                reconciliation.setStatus(ReconciliationStatus.PROCESSING.name());
                reconciliation.setStartTime(new Date());
                reconciliation.setEndTime(null);
                reconciliation.setErrorMessage(null);
                reconciliationMapper.updateById(reconciliation);
                reconciliationDetailMapper.delete(
                        new QueryWrapper<ReconciliationDetail>().eq("reconciliation_id", reconciliation.getId())
                );
            } else {
                reconciliation = new Reconciliation();
                reconciliation.setBillDate(billDate);
                reconciliation.setChannelCode(channelCode);
                reconciliation.setPaymentAppId(paymentAppId);
                reconciliation.setBillType(billType);
                reconciliation.setStatus(ReconciliationStatus.PROCESSING.name());
                reconciliation.setTotalCount(0);
                reconciliation.setMatchCount(0);
                reconciliation.setDiffCount(0);
                reconciliation.setDiffAmount(0);
                reconciliation.setChannelTotalAmount(0L);
                reconciliation.setLocalTotalAmount(0L);
                reconciliation.setStartTime(new Date());
                reconciliationMapper.insert(reconciliation);
            }

            try {
                doReconcile(reconciliation, billDate, channelCode, paymentAppId, billType);
                reconciliation.setStatus(ReconciliationStatus.COMPLETED.name());
                reconciliation.setEndTime(new Date());
            } catch (Exception e) {
                log.error("对账执行失败", e);
                reconciliation.setStatus(ReconciliationStatus.FAILED.name());
                reconciliation.setErrorMessage(truncateMessage(e.getMessage(), 1024));
                reconciliation.setEndTime(new Date());
            }

            reconciliationMapper.updateById(reconciliation);
            return reconciliationMapper.selectById(reconciliation.getId());

        } finally {
            stringRedisTemplate.delete(lockKey);
        }
    }

    private void doReconcile(Reconciliation reconciliation, LocalDate billDate,
                              String channelCode, Long paymentAppId, String billType) {
        // 对账依据：必须先导入渠道账单（渠道账单为T+1出账，当日无法对账）
        ChannelBill bill = channelBillMapper.selectByUniqueKey(billDate, channelCode, paymentAppId, billType);
        if (bill == null) {
            throw new BizException("渠道账单未导入，请先导入 " + billDate + " 的 " + channelCode
                    + " 账单（渠道账单为T+1出账，当日账单次日可导入）");
        }

        reconciliation.setBillId(bill.getId());
        reconciliation.setBillHash(bill.getBillHash());

        List<ChannelBillRecord> channelRecords = parseBill(bill.getBillContent(), channelCode);
        log.info("渠道账单解析完成，billId={}，共{}条记录", bill.getId(), channelRecords.size());

        List<ReconciliationDetail> details;
        if (CHANNEL_WXPAY.equals(channelCode)) {
            List<PaymentInfo> localPayments = queryLocalWxPayments(billDate);
            List<RefundInfo> localRefunds = queryLocalWxRefunds(billDate);
            Set<String> eligibleOrderNos = queryEligibleWxOrderNos(localPayments, localRefunds, paymentAppId);
            localPayments = localPayments.stream()
                    .filter(payment -> eligibleOrderNos.contains(payment.getOrderNo()))
                    .collect(Collectors.toList());
            localRefunds = localRefunds.stream()
                    .filter(refund -> eligibleOrderNos.contains(refund.getOrderNo()))
                    .collect(Collectors.toList());
            log.info("本地微信流水查询完成，进账{}条，退款{}条", localPayments.size(), localRefunds.size());
            details = wxPaymentRefundMatcher.match(
                    channelRecords, localPayments, localRefunds, reconciliation.getId());
        } else {
            List<OrderInfo> localOrders = queryLocalOrders(billDate, channelCode, paymentAppId);
            Map<String, PaymentInfo> paymentInfoMap = queryPaymentInfos(localOrders);
            log.info("本地订单查询完成，共{}条成功支付订单", localOrders.size());
            details = matchRecords(channelRecords, localOrders, paymentInfoMap, reconciliation.getId());
        }

        int matchCount = 0;
        int diffCount = 0;
        int diffAmount = 0;
        long channelTotalAmount = 0;
        long localTotalAmount = 0;

        for (ReconciliationDetail detail : details) {
            if (DiffType.MATCH.name().equals(detail.getDiffType())) {
                matchCount++;
            } else {
                diffCount++;
                if (detail.getDiffAmount() != null) {
                    diffAmount += Math.abs(detail.getDiffAmount());
                }
            }
            if (detail.getChannelAmount() != null) {
                channelTotalAmount += detail.getChannelAmount();
            }
            if (detail.getLocalAmount() != null) {
                localTotalAmount += detail.getLocalAmount();
            }
        }

        reconciliation.setTotalCount(details.size());
        reconciliation.setMatchCount(matchCount);
        reconciliation.setDiffCount(diffCount);
        reconciliation.setDiffAmount(diffAmount);
        reconciliation.setChannelTotalAmount(channelTotalAmount);
        reconciliation.setLocalTotalAmount(localTotalAmount);

        if (!details.isEmpty()) {
            reconciliationDetailMapper.batchInsert(details);
        }

        log.info("对账完成，总计{}条，匹配{}条，差异{}条，差异金额{}分",
                details.size(), matchCount, diffCount, diffAmount);
    }

    private List<ChannelBillRecord> parseBill(String billContent, String channelCode) {
        if (CHANNEL_WXPAY.equals(channelCode)) {
            return wxBillParser.parse(billContent);
        } else if (CHANNEL_ALIPAY.equals(channelCode)) {
            return aliPayBillParser.parse(billContent);
        }
        return Collections.emptyList();
    }

    private List<OrderInfo> queryLocalOrders(LocalDate billDate, String channelCode, Long paymentAppId) {
        Date startOfDay = Date.from(billDate.atStartOfDay(ZoneId.systemDefault()).toInstant());
        Date endOfDay = Date.from(billDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant());

        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("create_time", startOfDay)
                .lt("create_time", endOfDay)
                .eq("payment_channel_code", channelCode)
                .in("order_status", "支付成功", "退款中", "部分退款", "已退款", "退款异常");

        if (paymentAppId != null) {
            queryWrapper.eq("payment_app_id", paymentAppId);
        }

        return orderInfoMapper.selectList(queryWrapper);
    }

    private Map<String, PaymentInfo> queryPaymentInfos(List<OrderInfo> orders) {
        if (orders.isEmpty()) {
            return Collections.emptyMap();
        }
        List<String> orderNos = orders.stream().map(OrderInfo::getOrderNo).collect(Collectors.toList());
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("order_no", orderNos);
        List<PaymentInfo> paymentInfos = paymentInfoMapper.selectList(queryWrapper);
        return paymentInfos.stream()
                .collect(Collectors.toMap(PaymentInfo::getOrderNo, p -> p, (a, b) -> a));
    }

    private List<PaymentInfo> queryLocalWxPayments(LocalDate billDate) {
        Date startOfDay = Date.from(billDate.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
        Date endOfDay = Date.from(billDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
        QueryWrapper<PaymentInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("create_time", startOfDay)
                .lt("create_time", endOfDay)
                .eq("payment_type", PayType.WXPAY.getType());
        return paymentInfoMapper.selectList(queryWrapper);
    }

    private List<RefundInfo> queryLocalWxRefunds(LocalDate billDate) {
        Date startOfDay = Date.from(billDate.atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
        Date endOfDay = Date.from(billDate.plusDays(1).atStartOfDay(ZoneId.of("Asia/Shanghai")).toInstant());
        QueryWrapper<RefundInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.ge("approved_time", startOfDay)
                .lt("approved_time", endOfDay)
                .eq("approval_status", RefundApprovalStatus.APPROVED.getType());
        return refundInfoMapper.selectList(queryWrapper);
    }

    private Set<String> queryEligibleWxOrderNos(List<PaymentInfo> payments,
                                                 List<RefundInfo> refunds,
                                                 Long paymentAppId) {
        Set<String> orderNos = new HashSet<>();
        for (PaymentInfo payment : payments) {
            if (StringUtils.hasText(payment.getOrderNo())) {
                orderNos.add(payment.getOrderNo());
            }
        }
        for (RefundInfo refund : refunds) {
            if (StringUtils.hasText(refund.getOrderNo())) {
                orderNos.add(refund.getOrderNo());
            }
        }
        if (orderNos.isEmpty()) {
            return Collections.emptySet();
        }

        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.in("order_no", orderNos)
                .eq("payment_channel_code", CHANNEL_WXPAY);
        if (paymentAppId != null) {
            queryWrapper.eq("payment_app_id", paymentAppId);
        }
        return orderInfoMapper.selectList(queryWrapper).stream()
                .map(OrderInfo::getOrderNo)
                .filter(StringUtils::hasText)
                .collect(Collectors.toSet());
    }

    private List<ReconciliationDetail> matchRecords(List<ChannelBillRecord> channelRecords,
                                                      List<OrderInfo> localOrders,
                                                      Map<String, PaymentInfo> paymentInfoMap,
                                                      Long reconciliationId) {
        List<ReconciliationDetail> details = new ArrayList<>();

        Map<String, ChannelBillRecord> channelByOrderNo = new HashMap<>();
        Map<String, ChannelBillRecord> channelByTransactionId = new HashMap<>();
        for (ChannelBillRecord record : channelRecords) {
            if (StringUtils.hasText(record.getOrderNo())) {
                channelByOrderNo.put(record.getOrderNo(), record);
            }
            if (StringUtils.hasText(record.getTransactionId())) {
                channelByTransactionId.put(record.getTransactionId(), record);
            }
        }

        Set<String> matchedChannelKeys = new HashSet<>();

        for (OrderInfo order : localOrders) {
            PaymentInfo paymentInfo = paymentInfoMap.get(order.getOrderNo());
            ChannelBillRecord channelRecord = null;

            if (StringUtils.hasText(order.getOrderNo())) {
                channelRecord = channelByOrderNo.get(order.getOrderNo());
            }
            if (channelRecord == null && paymentInfo != null && StringUtils.hasText(paymentInfo.getTransactionId())) {
                channelRecord = channelByTransactionId.get(paymentInfo.getTransactionId());
            }

            ReconciliationDetail detail = new ReconciliationDetail();
            detail.setReconciliationId(reconciliationId);
            detail.setOrderNo(order.getOrderNo());
            detail.setLocalAmount(order.getTotalFee());
            detail.setLocalStatus(order.getOrderStatus());
            detail.setLocalTradeTime(order.getCreateTime());

            if (paymentInfo != null) {
                detail.setTransactionId(paymentInfo.getTransactionId());
            }

            if (channelRecord != null) {
                detail.setChannelAmount(channelRecord.getAmount());
                detail.setChannelStatus(channelRecord.getStatus());
                detail.setChannelTradeTime(channelRecord.getTradeTime());
                if (!StringUtils.hasText(detail.getTransactionId())) {
                    detail.setTransactionId(channelRecord.getTransactionId());
                }

                if (StringUtils.hasText(channelRecord.getOrderNo())) {
                    matchedChannelKeys.add(channelRecord.getOrderNo());
                }
                if (StringUtils.hasText(channelRecord.getTransactionId())) {
                    matchedChannelKeys.add(channelRecord.getTransactionId());
                }

                boolean amountMatch = Objects.equals(detail.getChannelAmount(), detail.getLocalAmount());
                boolean statusMatch = matchStatus(detail.getChannelStatus(), detail.getLocalStatus());

                if (amountMatch && statusMatch) {
                    detail.setDiffType(DiffType.MATCH.name());
                } else if (!amountMatch && !statusMatch) {
                    detail.setDiffType(DiffType.AMOUNT_MISMATCH.name());
                    detail.setDiffAmount(Math.abs(
                            (detail.getChannelAmount() == null ? 0 : detail.getChannelAmount()) -
                            (detail.getLocalAmount() == null ? 0 : detail.getLocalAmount())
                    ));
                    detail.setRemark("金额和状态均不匹配");
                } else if (!amountMatch) {
                    detail.setDiffType(DiffType.AMOUNT_MISMATCH.name());
                    detail.setDiffAmount(Math.abs(
                            (detail.getChannelAmount() == null ? 0 : detail.getChannelAmount()) -
                            (detail.getLocalAmount() == null ? 0 : detail.getLocalAmount())
                    ));
                    detail.setRemark("金额不匹配");
                } else {
                    detail.setDiffType(DiffType.STATUS_MISMATCH.name());
                    detail.setRemark("状态不匹配，渠道:" + detail.getChannelStatus() + ", 本地:" + detail.getLocalStatus());
                }
            } else {
                detail.setDiffType(DiffType.MISSING_CHANNEL.name());
                detail.setRemark("本地有订单但渠道账单无记录");
            }

            details.add(detail);
        }

        for (ChannelBillRecord channelRecord : channelRecords) {
            boolean matched = false;
            if (StringUtils.hasText(channelRecord.getOrderNo()) && matchedChannelKeys.contains(channelRecord.getOrderNo())) {
                matched = true;
            }
            if (!matched && StringUtils.hasText(channelRecord.getTransactionId())
                    && matchedChannelKeys.contains(channelRecord.getTransactionId())) {
                matched = true;
            }

            if (!matched) {
                ReconciliationDetail detail = new ReconciliationDetail();
                detail.setReconciliationId(reconciliationId);
                detail.setOrderNo(channelRecord.getOrderNo());
                detail.setTransactionId(channelRecord.getTransactionId());
                detail.setChannelAmount(channelRecord.getAmount());
                detail.setChannelStatus(channelRecord.getStatus());
                detail.setChannelTradeTime(channelRecord.getTradeTime());
                detail.setDiffType(DiffType.MISSING_LOCAL.name());
                detail.setRemark("渠道账单有记录但本地无对应订单");
                details.add(detail);
            }
        }

        return details;
    }

    private boolean matchStatus(String channelStatus, String localStatus) {
        if (!StringUtils.hasText(channelStatus) || !StringUtils.hasText(localStatus)) {
            return false;
        }
        Set<String> successStatuses = new HashSet<>(Arrays.asList(
                "SUCCESS", "支付成功", "TRADE_SUCCESS", "交易成功"
        ));
        Set<String> refundStatuses = new HashSet<>(Arrays.asList(
                "退款中", "部分退款", "已退款", "退款异常", "REFUND"
        ));

        boolean channelSuccess = successStatuses.contains(channelStatus);
        boolean localSuccess = successStatuses.contains(localStatus) || "支付成功".equals(localStatus);
        if (channelSuccess && localSuccess) {
            return true;
        }

        boolean channelRefund = refundStatuses.contains(channelStatus);
        boolean localRefund = refundStatuses.contains(localStatus);
        if (channelRefund && localRefund) {
            return true;
        }

        return channelStatus.equalsIgnoreCase(localStatus);
    }

    @Override
    public IPage<Reconciliation> listReconciliation(int pageNum, int pageSize,
                                                       LocalDate billDateStart, LocalDate billDateEnd,
                                                       String channelCode, String status) {
        Page<Reconciliation> page = new Page<>(pageNum, pageSize);
        return reconciliationMapper.selectPageByConditions(page, billDateStart, billDateEnd, channelCode, status);
    }

    @Override
    public Reconciliation getReconciliationById(Long id) {
        return reconciliationMapper.selectById(id);
    }

    @Override
    public IPage<ReconciliationDetail> listDetails(Long reconciliationId, int pageNum, int pageSize, String diffType) {
        Page<ReconciliationDetail> page = new Page<>(pageNum, pageSize);
        return reconciliationDetailMapper.selectPageByReconciliationId(page, reconciliationId, diffType);
    }

    @Override
    public IPage<ReconciliationDetail> listDiffDetails(Long reconciliationId, int pageNum, int pageSize) {
        Page<ReconciliationDetail> page = new Page<>(pageNum, pageSize);
        return reconciliationDetailMapper.selectDiffPage(page, reconciliationId);
    }

    @Override
    public String exportReconciliation(Long id) {
        Reconciliation reconciliation = reconciliationMapper.selectById(id);
        if (reconciliation == null) {
            throw new BizException("对账记录不存在");
        }

        List<ReconciliationDetail> details = reconciliationDetailMapper.selectAllByReconciliationId(id);

        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(baos, StandardCharsets.UTF_8)) {

            writer.write('\ufeff');
            writer.write("业务类型,差异类型,商户订单号,渠道交易号,商户退款单号,渠道退款单号,渠道金额(分),本地金额(分),差异金额(分),渠道状态,本地状态,备注\n");

            for (ReconciliationDetail detail : details) {
                writer.write(String.format("%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s,%s\n",
                        escapeCsv(detail.getBusinessType()),
                        escapeCsv(detail.getDiffType()),
                        escapeCsv(detail.getOrderNo()),
                        escapeCsv(detail.getTransactionId()),
                        escapeCsv(detail.getRefundNo()),
                        escapeCsv(detail.getRefundId()),
                        detail.getChannelAmount() == null ? "" : detail.getChannelAmount(),
                        detail.getLocalAmount() == null ? "" : detail.getLocalAmount(),
                        detail.getDiffAmount() == null ? "" : detail.getDiffAmount(),
                        escapeCsv(detail.getChannelStatus()),
                        escapeCsv(detail.getLocalStatus()),
                        escapeCsv(detail.getRemark())
                ));
            }

            writer.flush();
            return baos.toString(StandardCharsets.UTF_8.name());
        } catch (Exception e) {
            throw new BizException("导出对账报告失败", e);
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        return value;
    }

    private LocalDate parseBillDate(String billDateStr) {
        try {
            return LocalDate.parse(billDateStr);
        } catch (DateTimeParseException e) {
            throw new BizException("账单日期格式必须为yyyy-MM-dd");
        }
    }

    private void validateChannelCode(String channelCode) {
        if (!CHANNEL_WXPAY.equals(channelCode) && !CHANNEL_ALIPAY.equals(channelCode)) {
            throw new BizException("渠道编码只支持WXPAY或ALIPAY");
        }
    }

    private String buildLockKey(LocalDate billDate, String channelCode, Long paymentAppId) {
        return RECON_LOCK_PREFIX + billDate + ":" + channelCode + ":" + (paymentAppId == null ? "default" : paymentAppId);
    }

    private String truncateMessage(String message, int maxLength) {
        if (message == null) {
            return null;
        }
        return message.length() > maxLength ? message.substring(0, maxLength) : message;
    }
}
