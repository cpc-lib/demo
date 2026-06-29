package cc.ivera.characterization;

import cc.ivera.config.OrderCloseRabbitConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.controller.WxPayV2Controller;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.mapper.PaymentInfoMapper;
import cc.ivera.mapper.RefundInfoMapper;
import cc.ivera.mq.OrderCloseMessage;
import cc.ivera.service.AliPayService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentAppService;
import cc.ivera.service.PaymentChannelService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.impl.OrderCloseMessageServiceImpl;
import cc.ivera.service.impl.PaymentInfoServiceImpl;
import cc.ivera.service.refund.OrderRefundStatusService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import com.fasterxml.jackson.databind.ObjectMapper;
import cc.ivera.lock.DistributedLockTemplate;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataIntegrityViolationException;
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
    @DisplayName("current_wxpay_v2_notify_bad_xml_returns_fail_without_touching_state")
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
                distributedLockTemplate,
                transactionTemplate,
                stringRedisTemplate);
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("<xml><broken>".getBytes(StandardCharsets.UTF_8));

        String response = controller.wxNotify(request);

        assertThat(response).contains("FAIL");
        assertThat(response).contains("\u5931\u8d25");
        verifyNoInteractions(stringRedisTemplate);
        verifyNoInteractions(distributedLockTemplate);
        verifyNoInteractions(orderInfoService);
        verifyNoInteractions(paymentInfoService);
    }

    @Test
    @DisplayName("current_payment_info_duplicate_key_is_swallowed_and_logged")
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
        assertThat(output.getOut()).contains("\u652f\u4ed8\u6d41\u6c34\u5df2\u5b58\u5728");
        assertThat(output.getOut()).contains("ORD-DUP");
        assertThat(output.getOut()).contains("TX-DUP");
    }

    @Test
    @DisplayName("dm8_payment_config_loader_startup_missing_tables_does_not_abort_context")
    void dm8_payment_config_loader_startup_missing_tables_does_not_abort_context(CapturedOutput output) {
        PaymentAppService paymentAppService = mock(PaymentAppService.class);
        PaymentChannelService paymentChannelService = mock(PaymentChannelService.class);
        when(paymentChannelService.listEnabledChannels())
                .thenThrow(new DataIntegrityViolationException("invalid table t_payment_channel"));
        PaymentConfigLoader loader = new PaymentConfigLoader(
                paymentAppService,
                paymentChannelService,
                new ObjectMapper());

        assertThatCode(loader::init).doesNotThrowAnyException();

        assertThat(loader.getAllAppConfigs()).isEmpty();
        assertThat(output.getOut()).contains("Payment config tables are not available during startup");
        assertThat(output.getOut()).doesNotContain("org.springframework.dao.DataIntegrityViolationException");
        verify(paymentChannelService).listEnabledChannels();
        verifyNoInteractions(paymentAppService);
        assertThatThrownBy(loader::reloadConfigs).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("current_alipay_payment_info_converts_yuan_to_cents_and_sets_trade_type")
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
        assertThat(paymentInfo.getTradeType()).isEqualTo("\u7535\u8111\u7f51\u7ad9\u652f\u4ed8");
        assertThat(paymentInfo.getTradeState()).isEqualTo("TRADE_SUCCESS");
        assertThat(paymentInfo.getPayerTotal()).isEqualTo(1234);
    }

    @Test
    @DisplayName("current_order_close_message_uses_fixed_exchange_and_routing_key")
    void current_order_close_message_uses_fixed_exchange_and_routing_key() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OrderCloseMessageServiceImpl service = new OrderCloseMessageServiceImpl(rabbitTemplate);

        service.sendCloseOrderMessage("ORD-CLOSE", PayType.WXPAY.getType());

        ArgumentCaptor<OrderCloseMessage> captor = ArgumentCaptor.forClass(OrderCloseMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(OrderCloseRabbitConfig.ORDER_CLOSE_EVENT_EXCHANGE),
                eq(OrderCloseRabbitConfig.ORDER_CLOSE_DELAY_ROUTING_KEY),
                captor.capture());
        assertThat(captor.getValue().getOrderNo()).isEqualTo("ORD-CLOSE");
        assertThat(captor.getValue().getPaymentType()).isEqualTo(PayType.WXPAY.getType());
    }

    @Test
    @DisplayName("current_order_close_message_rejects_blank_order_or_payment_type")
    void current_order_close_message_rejects_blank_order_or_payment_type() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OrderCloseMessageServiceImpl service = new OrderCloseMessageServiceImpl(rabbitTemplate);

        assertThatThrownBy(() -> service.sendCloseOrderMessage("", PayType.WXPAY.getType()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u8ba2\u5355\u53f7\u6216\u652f\u4ed8\u7c7b\u578b\u4e3a\u7a7a");
        assertThatThrownBy(() -> service.sendCloseOrderMessage("ORD-CLOSE", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("\u8ba2\u5355\u53f7\u6216\u652f\u4ed8\u7c7b\u578b\u4e3a\u7a7a");
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(OrderCloseMessage.class));
    }

    @Test
    @DisplayName("current_refund_status_summary_full_success_wins")
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
    @DisplayName("current_refund_status_summary_partial_success_wins_over_processing")
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
    @DisplayName("current_refund_status_summary_processing_when_no_success")
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
    @DisplayName("current_refund_status_summary_abnormal_when_only_abnormal")
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
