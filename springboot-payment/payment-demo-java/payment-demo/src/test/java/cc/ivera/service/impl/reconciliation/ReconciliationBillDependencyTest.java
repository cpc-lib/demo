package cc.ivera.service.impl.reconciliation;

import cc.ivera.dto.ReconciliationExecuteRequest;
import cc.ivera.entity.ChannelBill;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentInfo;
import cc.ivera.entity.Reconciliation;
import cc.ivera.entity.ReconciliationDetail;
import cc.ivera.entity.RefundInfo;
import cc.ivera.mapper.ChannelBillMapper;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.PaymentInfoMapper;
import cc.ivera.mapper.ReconciliationDetailMapper;
import cc.ivera.mapper.ReconciliationMapper;
import cc.ivera.mapper.RefundInfoMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;

import java.time.LocalDate;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("对账依赖已导入账单测试")
class ReconciliationBillDependencyTest {

    private ReconciliationMapper reconciliationMapper;
    private ReconciliationDetailMapper reconciliationDetailMapper;
    private ChannelBillMapper channelBillMapper;
    private OrderInfoMapper orderInfoMapper;
    private PaymentInfoMapper paymentInfoMapper;
    private RefundInfoMapper refundInfoMapper;
    private ReconciliationServiceImpl reconciliationService;
    private final AtomicReference<Reconciliation> inserted = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        reconciliationMapper = mock(ReconciliationMapper.class);
        reconciliationDetailMapper = mock(ReconciliationDetailMapper.class);
        channelBillMapper = mock(ChannelBillMapper.class);
        orderInfoMapper = mock(OrderInfoMapper.class);
        paymentInfoMapper = mock(PaymentInfoMapper.class);
        refundInfoMapper = mock(RefundInfoMapper.class);

        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(redisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class))).thenReturn(true);

        reconciliationService = new ReconciliationServiceImpl(
                reconciliationMapper, reconciliationDetailMapper, channelBillMapper,
                orderInfoMapper, paymentInfoMapper, refundInfoMapper,
                new WxPaymentRefundMatcher(),
                new WxBillParser(), new AliPayBillParser(), redisTemplate);

        when(paymentInfoMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(refundInfoMapper.selectList(any())).thenReturn(Collections.emptyList());
        when(orderInfoMapper.selectList(any())).thenReturn(Collections.emptyList());

        inserted.set(null);
        when(reconciliationMapper.insert(any(Reconciliation.class))).thenAnswer(invocation -> {
            Reconciliation reconciliation = invocation.getArgument(0);
            reconciliation.setId(100L);
            inserted.set(reconciliation);
            return 1;
        });
        when(reconciliationMapper.selectById(any())).thenAnswer(invocation -> inserted.get());
        when(reconciliationMapper.updateById(any(Reconciliation.class))).thenReturn(1);
        when(reconciliationMapper.selectByUniqueKey(any(), anyString(), any(), anyString())).thenReturn(null);
    }

    private ReconciliationExecuteRequest executeRequest(String billDate, String channelCode) {
        ReconciliationExecuteRequest request = new ReconciliationExecuteRequest();
        request.setBillDate(billDate);
        request.setChannelCode(channelCode);
        return request;
    }

    private String yesterday() {
        return LocalDate.now().minusDays(1).toString();
    }

    private String wxCsv() {
        return "交易时间,公众账号ID,商户号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率\n" +
                "2026-08-27 10:30:00,wx_appid,1234567890,4200001234202608271234567890,ORDER_DEP_001,user_openid,NATIVE,SUCCESS,OTHERS,CNY,1.00,0.00,,,,,,,,测试商品,0.010,0.1%\n" +
                "2026-08-27 11:20:00,wx_appid,1234567890,4200001234202608270987654321,ORDER_DEP_002,user_openid2,NATIVE,SUCCESS,OTHERS,CNY,2.50,0.00,,,,,,,,测试商品2,0.025,0.1%\n";
    }

    @Test
    @DisplayName("【现状】账单未导入时执行对账：记录FAILED并提示先导入账单（含T+1说明）")
    void testExecuteWithoutImportedBillFails() {
        when(channelBillMapper.selectByUniqueKey(any(), anyString(), any(), anyString())).thenReturn(null);

        Reconciliation result = reconciliationService.executeReconciliation(
                executeRequest(yesterday(), "WXPAY"));

        assertThat(result.getStatus()).isEqualTo("FAILED");
        assertThat(result.getErrorMessage())
                .contains("渠道账单未导入")
                .contains(yesterday())
                .contains("WXPAY")
                .contains("T+1");
    }

    @Test
    @DisplayName("【现状】账单已导入时执行对账：关联billId并沿用账单哈希")
    void testExecuteWithImportedBillReconciles() {
        ChannelBill bill = new ChannelBill();
        bill.setId(9L);
        bill.setBillDate(LocalDate.parse(yesterday()));
        bill.setChannelCode("WXPAY");
        bill.setBillContent(wxCsv());
        bill.setBillHash("hash-of-bill");
        when(channelBillMapper.selectByUniqueKey(any(), eq("WXPAY"), any(), eq("ALL"))).thenReturn(bill);
        // 本地无订单：渠道账单2条全部为漏单(MISSING_LOCAL)
        when(orderInfoMapper.selectList(any())).thenReturn(Collections.emptyList());

        Reconciliation result = reconciliationService.executeReconciliation(
                executeRequest(yesterday(), "WXPAY"));

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getBillId()).isEqualTo(9L);
        assertThat(result.getBillHash()).isEqualTo("hash-of-bill");
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getDiffCount()).isEqualTo(2);
        assertThat(result.getChannelTotalAmount()).isEqualTo(350L);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReconciliationDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(reconciliationDetailMapper).batchInsert(captor.capture());
        assertThat(captor.getValue()).hasSize(2);
        assertThat(captor.getValue().get(0).getDiffType()).isEqualTo("MISSING_LOCAL");
    }

    @Test
    @DisplayName("【现状】重复执行对账：已完成的记录直接返回不重复执行")
    void testExecuteIdempotent() {
        ChannelBill bill = new ChannelBill();
        bill.setId(9L);
        bill.setBillContent(wxCsv());
        when(channelBillMapper.selectByUniqueKey(any(), eq("WXPAY"), any(), eq("ALL"))).thenReturn(bill);

        Reconciliation completed = new Reconciliation();
        completed.setId(100L);
        completed.setStatus("COMPLETED");
        when(reconciliationMapper.selectByUniqueKey(any(), eq("WXPAY"), any(), eq("ALL")))
                .thenReturn(completed);

        Reconciliation result = reconciliationService.executeReconciliation(
                executeRequest(yesterday(), "WXPAY"));

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        verify(reconciliationMapper, times(0)).insert(any(Reconciliation.class));
        verify(reconciliationDetailMapper, times(0)).batchInsert(any());
    }

    @Test
    @DisplayName("微信进账退款对账：支付流水和跨日原订单退款均完整匹配")
    void testReconcilePaymentInfoAndCrossDayRefundInfo() {
        ChannelBill bill = new ChannelBill();
        bill.setId(12L);
        bill.setBillDate(LocalDate.parse(yesterday()));
        bill.setChannelCode("WXPAY");
        bill.setBillContent(wxPaymentRefundCsv());
        bill.setBillHash("payment-refund-hash");
        when(channelBillMapper.selectByUniqueKey(any(), eq("WXPAY"), any(), eq("ALL"))).thenReturn(bill);

        PaymentInfo payment = new PaymentInfo();
        payment.setOrderNo("ORDER_FLOW_001");
        payment.setTransactionId("WX_FLOW_001");
        payment.setPaymentType("微信");
        payment.setTradeState("SUCCESS");
        payment.setPayerTotal(100);
        payment.setCreateTime(new Date());
        when(paymentInfoMapper.selectList(any())).thenReturn(Collections.singletonList(payment));

        RefundInfo refund = new RefundInfo();
        refund.setOrderNo("ORDER_FROM_PREVIOUS_DAY");
        refund.setRefundNo("REFUND_FLOW_001");
        refund.setRefundId("WX_REFUND_FLOW_001");
        refund.setRefund(60);
        refund.setApprovalStatus("APPROVED");
        refund.setRefundStatus("SUCCESS");
        refund.setApprovedTime(new Date());
        when(refundInfoMapper.selectList(any())).thenReturn(Collections.singletonList(refund));

        OrderInfo paymentOrder = wxOrder("ORDER_FLOW_001", new Date());
        OrderInfo oldRefundOrder = wxOrder("ORDER_FROM_PREVIOUS_DAY", new Date(0L));
        when(orderInfoMapper.selectList(any())).thenReturn(Arrays.asList(paymentOrder, oldRefundOrder));

        Reconciliation result = reconciliationService.executeReconciliation(
                executeRequest(yesterday(), "WXPAY"));

        assertThat(result.getStatus()).isEqualTo("COMPLETED");
        assertThat(result.getTotalCount()).isEqualTo(2);
        assertThat(result.getMatchCount()).isEqualTo(2);
        assertThat(result.getDiffCount()).isZero();

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ReconciliationDetail>> captor = ArgumentCaptor.forClass(List.class);
        verify(reconciliationDetailMapper).batchInsert(captor.capture());
        assertThat(captor.getValue()).extracting(ReconciliationDetail::getBusinessType)
                .containsExactly("PAYMENT", "REFUND");
        assertThat(captor.getValue()).extracting(ReconciliationDetail::getDiffType)
                .containsOnly("MATCH");
    }

    private OrderInfo wxOrder(String orderNo, Date createTime) {
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setPaymentType("微信");
        order.setPaymentChannelCode("WXPAY");
        order.setCreateTime(createTime);
        return order;
    }

    private String wxPaymentRefundCsv() {
        String headers = "交易时间,公众账号ID,商户号,特约商户号,设备号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率,订单金额,申请退款金额,费率备注";
        String payment = "`2026-08-28 10:00:00,`wx_appid,`1900000001,`,`device,`WX_FLOW_001,`ORDER_FLOW_001,`openid,`JSAPI,`SUCCESS,`CMB,`CNY,`0.99,`0.00,`0,`0,`0.00,`0.00,`,`,`商品,`,`0.00600,`0.60%,`1.00,`0.00,`";
        String refund = "`2026-08-28 11:00:00,`wx_appid,`1900000001,`,`device,`WX_OLD_FLOW,`ORDER_FROM_PREVIOUS_DAY,`openid,`JSAPI,`REFUND,`CMB,`CNY,`0.40,`0.00,`WX_REFUND_FLOW_001,`REFUND_FLOW_001,`0.40,`0.00,`ORIGINAL,`SUCCESS,`商品,`,`0.00000,`0.00%,`1.00,`0.60,`";
        return headers + "\n" + payment + "\n" + refund + "\n";
    }
}
