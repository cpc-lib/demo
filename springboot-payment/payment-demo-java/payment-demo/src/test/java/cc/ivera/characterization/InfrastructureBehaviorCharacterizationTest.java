package cc.ivera.characterization;

import cc.ivera.config.WxPayConfig;
import cc.ivera.controller.WxPayV2Controller;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.mapper.PaymentInfoMapper;
import cc.ivera.mapper.RefundInfoMapper;
import cc.ivera.mq.OutboxEventTypes;
import cc.ivera.service.AliPayService;
import cc.ivera.service.MessageOutboxService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.impl.OrderCloseMessageServiceImpl;
import cc.ivera.service.impl.PaymentInfoServiceImpl;
import cc.ivera.service.refund.OrderRefundStatusService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.util.JsonUtils;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.HashMap;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class InfrastructureBehaviorCharacterizationTest {

    @Test
    @DisplayName("现状: 微信 V2 通知 XML 解析失败时返回 FAIL XML 且不触发缓存/锁/DB")
    void current_wxpay_v2_notify_bad_xml_returns_fail_without_touching_state() {
        WxPayOrderFacade wxPayOrderFacade = mock(WxPayOrderFacade.class);
        WxPayConfig wxPayConfig = mock(WxPayConfig.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        PaymentInfoService paymentInfoService = mock(PaymentInfoService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        WxPayV2Controller controller = new WxPayV2Controller(
                wxPayOrderFacade,
                wxPayConfig,
                orderInfoService,
                paymentInfoService,
                mock(cc.ivera.service.InventoryService.class),
                distributedLockTemplate,
                transactionTemplate,
                stringRedisTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("<xml><broken>".getBytes(StandardCharsets.UTF_8));

        String response = controller.wxNotify(request);

        assertThat(response).contains("FAIL");
        assertThat(response).contains("失败");
        verifyNoInteractions(stringRedisTemplate);
        verifyNoInteractions(distributedLockTemplate);
        verifyNoInteractions(orderInfoService);
        verifyNoInteractions(paymentInfoService);
    }

    @Test
    @DisplayName("现状: 支付流水唯一约束冲突会被吞掉并记录幂等日志")
    void current_payment_info_duplicate_key_is_swallowed_and_logged(CapturedOutput output) {
        PaymentInfoMapper paymentInfoMapper = mock(PaymentInfoMapper.class);
        when(paymentInfoMapper.insert(any(PaymentInfo.class))).thenThrow(new DuplicateKeyException("duplicate"));
        PaymentInfoServiceImpl service = new PaymentInfoServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", paymentInfoMapper);
        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORD-DUP");
        params.put("transaction_id", "TX-DUP");
        params.put("trade_type", "NATIVE");
        params.put("result_code", "SUCCESS");
        params.put("total_fee", "1");

        assertThatCode(() -> service.createPaymentInfoForWxPayV2(params, "<xml/>")).doesNotThrowAnyException();

        verify(paymentInfoMapper).insert(any(PaymentInfo.class));
        assertThat(output.getOut()).contains("支付流水已存在");
        assertThat(output.getOut()).contains("ORD-DUP");
        assertThat(output.getOut()).contains("TX-DUP");
    }

    @Test
    @DisplayName("现状: 支付宝支付流水将元转换为分并固定 tradeType=电脑网站支付")
    void current_alipay_payment_info_converts_yuan_to_cents_and_sets_trade_type() {
        PaymentInfoMapper paymentInfoMapper = mock(PaymentInfoMapper.class);
        PaymentInfoServiceImpl service = new PaymentInfoServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", paymentInfoMapper);
        Map<String, Object> params = new HashMap<>();
        params.put("out_trade_no", "ORD-ALI");
        params.put("trade_no", "ALI-TX");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("total_amount", "12.34");

        service.createPaymentInfoForAliPay(params);

        ArgumentCaptor<PaymentInfo> captor = ArgumentCaptor.forClass(PaymentInfo.class);
        verify(paymentInfoMapper).insert(captor.capture());
        PaymentInfo paymentInfo = captor.getValue();
        assertThat(paymentInfo.getOrderNo()).isEqualTo("ORD-ALI");
        assertThat(paymentInfo.getTransactionId()).isEqualTo("ALI-TX");
        assertThat(paymentInfo.getPaymentType()).isEqualTo(PayType.ALIPAY.getType());
        assertThat(paymentInfo.getTradeType()).isEqualTo("电脑网站支付");
        assertThat(paymentInfo.getTradeState()).isEqualTo("TRADE_SUCCESS");
        assertThat(paymentInfo.getPayerTotal()).isEqualTo(1234);
    }

    @Test
    @DisplayName("关单事件先按订单号幂等写入 Outbox")
    void order_close_message_is_persisted_to_outbox() {
        MessageOutboxService outboxService = mock(MessageOutboxService.class);
        OrderCloseMessageServiceImpl service = new OrderCloseMessageServiceImpl(outboxService);

        service.sendCloseOrderMessage("ORD-CLOSE", PayType.WXPAY.getType());

        ArgumentCaptor<String> payload = ArgumentCaptor.forClass(String.class);
        verify(outboxService).insertOnce(
                eq("ORDER_CLOSE_SCHEDULED:ORD-CLOSE"),
                eq("ORDER"),
                eq("ORD-CLOSE"),
                eq(OutboxEventTypes.ORDER_CLOSE_SCHEDULED),
                payload.capture());
        assertThat(JsonUtils.toObjectMap(payload.getValue()))
                .containsEntry("orderNo", "ORD-CLOSE")
                .containsEntry("paymentType", PayType.WXPAY.getType());
    }

    @Test
    @DisplayName("现状: 延迟关单事件缺少订单号或支付类型时抛 IllegalArgumentException")
    void current_order_close_message_rejects_blank_order_or_payment_type() {
        MessageOutboxService outboxService = mock(MessageOutboxService.class);
        OrderCloseMessageServiceImpl service = new OrderCloseMessageServiceImpl(outboxService);

        assertThatThrownBy(() -> service.sendCloseOrderMessage("", PayType.WXPAY.getType()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单号或支付类型为空");
        assertThatThrownBy(() -> service.sendCloseOrderMessage("ORD-CLOSE", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单号或支付类型为空");
        verifyNoInteractions(outboxService);
    }

    @Test
    @DisplayName("现状: 退款汇总成功金额达到订单金额时订单变为 REFUND_SUCCESS")
    void current_refund_status_summary_full_success_wins() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);
        OrderInfo orderInfo = paidOrder(100);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-REFUND")).thenReturn(orderInfo);
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(anyString(), any())).thenReturn(100, 0, 0);

        service.refreshOrderRefundStatus("ORD-REFUND");

        verify(orderInfoService).updateStatusByOrderNo("ORD-REFUND", OrderStatus.REFUND_SUCCESS);
    }

    @Test
    @DisplayName("现状: 退款汇总存在部分成功时优先于处理中状态")
    void current_refund_status_summary_partial_success_wins_over_processing() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);
        OrderInfo orderInfo = paidOrder(100);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-REFUND")).thenReturn(orderInfo);
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(anyString(), any())).thenReturn(10, 80, 0);

        service.refreshOrderRefundStatus("ORD-REFUND");

        verify(orderInfoService).updateStatusByOrderNo("ORD-REFUND", OrderStatus.PARTIAL_REFUND);
    }

    @Test
    @DisplayName("现状: 退款汇总无成功但有处理中时订单变为 REFUND_PROCESSING")
    void current_refund_status_summary_processing_when_no_success() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);
        OrderInfo orderInfo = paidOrder(100);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-REFUND")).thenReturn(orderInfo);
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(anyString(), any())).thenReturn(0, 80, 20);

        service.refreshOrderRefundStatus("ORD-REFUND");

        verify(orderInfoService).updateStatusByOrderNo("ORD-REFUND", OrderStatus.REFUND_PROCESSING);
    }

    @Test
    @DisplayName("现状: 退款汇总无成功无处理中但有异常时订单变为 REFUND_ABNORMAL")
    void current_refund_status_summary_abnormal_when_only_abnormal() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);
        OrderInfo orderInfo = paidOrder(100);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-REFUND")).thenReturn(orderInfo);
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(anyString(), any())).thenReturn(0, 0, 20);

        service.refreshOrderRefundStatus("ORD-REFUND");

        verify(orderInfoService).updateStatusByOrderNo("ORD-REFUND", OrderStatus.REFUND_ABNORMAL);
    }

    private OrderInfo paidOrder(int totalFee) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo("ORD-REFUND");
        orderInfo.setTotalFee(totalFee);
        orderInfo.setOrderStatus(OrderStatus.SUCCESS.getType());
        return orderInfo;
    }
}
