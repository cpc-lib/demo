package cc.ivera.service.impl;

import cc.ivera.config.AlipayProperties;
import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentOrder;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.InventoryService;
import cc.ivera.service.MessageConsumeLogService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.impl.wxpay.WxPayHttpClient;
import cc.ivera.service.impl.wxpay.WxPayNotificationDecoder;
import cc.ivera.service.impl.wxpay.WxPayOrderService;
import com.alipay.api.AlipayClient;
import com.alipay.api.request.AlipayTradePagePayRequest;
import com.alipay.api.response.AlipayTradePagePayResponse;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.spy;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PaymentAttemptChannelCreationTest {

    @Test
    void wxV3UsesPaymentNumberAsMerchantNumberWithoutWritingLegacyOrderCodeUrl() throws Exception {
        PaymentConfigLoader loader = mock(PaymentConfigLoader.class);
        PaymentAppConfig config = wxConfig();
        when(loader.getRequiredAppConfig(9L)).thenReturn(config);
        WxPayHttpClient httpClient = mock(WxPayHttpClient.class);
        when(httpClient.postJson(any(PaymentAppConfig.class), anyString(), anyString(), anyString()))
                .thenReturn("{\"code_url\":\"wx://pay\"}");
        OrderInfoService orderService = mock(OrderInfoService.class);
        WxPayOrderService service = new WxPayOrderService(
                mock(WxPayConfig.class),
                loader,
                orderService,
                mock(PaymentInfoService.class),
                mock(InventoryService.class),
                httpClient,
                mock(WxPayNotificationDecoder.class),
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class),
                mock(MessageConsumeLogService.class)
        );
        PaymentOrder payment = payment("WXPAY", "V3");
        OrderInfo order = order();

        Map<String, Object> result = service.createPayment(payment, order, "127.0.0.1");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(httpClient).postJson(any(PaymentAppConfig.class), anyString(), body.capture(), anyString());
        assertTrue(body.getValue().contains("\"out_trade_no\":\"PAY-1\""));
        assertFalse(body.getValue().contains("\"out_trade_no\":\"ORDER-1\""));
        assertEquals("PAY-1", result.get("paymentNo"));
        assertEquals("ORDER-1", result.get("orderNo"));
        assertEquals("wx://pay", result.get("codeUrl"));
        verify(orderService, never()).saveCodeUrl(anyString(), anyString());
    }

    @Test
    void wxV3PreservesLongCentAmountWithoutNarrowingToInteger() throws Exception {
        PaymentConfigLoader loader = mock(PaymentConfigLoader.class);
        when(loader.getRequiredAppConfig(9L)).thenReturn(wxConfig());
        WxPayHttpClient httpClient = mock(WxPayHttpClient.class);
        when(httpClient.postJson(any(PaymentAppConfig.class), anyString(), anyString(), anyString()))
                .thenReturn("{\"code_url\":\"wx://pay\"}");
        WxPayOrderService service = new WxPayOrderService(
                mock(WxPayConfig.class), loader, mock(OrderInfoService.class),
                mock(PaymentInfoService.class), mock(InventoryService.class), httpClient,
                mock(WxPayNotificationDecoder.class), mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class), mock(MessageConsumeLogService.class));
        PaymentOrder payment = payment("WXPAY", "V3");
        payment.setRequestAmount(3_000_000_000L);
        OrderInfo order = order();
        order.setPayableAmount(3_000_000_000L);

        service.createPayment(payment, order, "127.0.0.1");

        ArgumentCaptor<String> body = ArgumentCaptor.forClass(String.class);
        verify(httpClient).postJson(any(PaymentAppConfig.class), anyString(), body.capture(), anyString());
        assertTrue(body.getValue().contains("\"total\":3000000000"));
    }

    @Test
    void alipayUsesPaymentNumberAsMerchantNumber() throws Exception {
        PaymentConfigLoader loader = mock(PaymentConfigLoader.class);
        PaymentAppConfig config = aliConfig();
        when(loader.getRequiredAppConfig(9L)).thenReturn(config);
        AlipayClient client = mock(AlipayClient.class);
        AlipayTradePagePayResponse response = mock(AlipayTradePagePayResponse.class);
        when(response.isSuccess()).thenReturn(true);
        when(response.getBody()).thenReturn("<form>pay</form>");
        when(client.pageExecute(any(AlipayTradePagePayRequest.class))).thenReturn(response);
        AliPayServiceImpl service = spy(new AliPayServiceImpl(
                mock(OrderInfoService.class),
                mock(AlipayClient.class),
                mock(AlipayProperties.class),
                loader,
                mock(PaymentInfoService.class),
                mock(InventoryService.class),
                mock(RefundInfoService.class),
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class),
                mock(MessageConsumeLogService.class)
        ));
        doReturn(client).when(service).buildAlipayClient(config);

        assertEquals("<form>pay</form>", service.createPayment(
                payment("ALIPAY", "ALIPAY"), order()));

        ArgumentCaptor<AlipayTradePagePayRequest> request =
                ArgumentCaptor.forClass(AlipayTradePagePayRequest.class);
        verify(client).pageExecute(request.capture());
        assertTrue(request.getValue().getBizContent().contains("\"out_trade_no\":\"PAY-1\""));
        assertFalse(request.getValue().getBizContent().contains("\"out_trade_no\":\"ORDER-1\""));
    }

    private PaymentOrder payment(String channel, String protocol) {
        PaymentOrder payment = new PaymentOrder();
        payment.setPaymentNo("PAY-1");
        payment.setChannelMerchantOrderNo("PAY-1");
        payment.setOrderNo("ORDER-1");
        payment.setPaymentAppId(9L);
        payment.setChannel(channel);
        payment.setProtocol(protocol);
        payment.setRequestAmount(1000L);
        return payment;
    }

    private OrderInfo order() {
        OrderInfo order = new OrderInfo();
        order.setOrderNo("ORDER-1");
        order.setTitle("Java");
        order.setTotalFee(1000);
        order.setPayableAmount(1000L);
        return order;
    }

    private PaymentAppConfig wxConfig() {
        PaymentAppConfig config = new PaymentAppConfig();
        config.setAppId(9L);
        config.setChannelCode("WXPAY");
        config.setAppid("wx-app");
        config.setMchId("merchant");
        config.setDomain("https://wx.example");
        config.setNotifyUrl("https://notify.example");
        return config;
    }

    private PaymentAppConfig aliConfig() {
        PaymentAppConfig config = new PaymentAppConfig();
        config.setAppId(9L);
        config.setChannelCode("ALIPAY");
        config.setGatewayUrl("https://ali.example");
        config.setAlipayAppId("ali-app");
        config.setMerchantPrivateKey("private");
        config.setAlipayPublicKey("public");
        config.setNotifyUrl("https://notify.example");
        config.setAlipayNotifyUrl("https://notify.example");
        config.setReturnUrl("https://return.example");
        return config;
    }
}
