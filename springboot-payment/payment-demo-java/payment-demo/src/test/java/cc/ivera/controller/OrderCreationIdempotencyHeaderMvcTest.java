package cc.ivera.controller;

import cc.ivera.config.AlipayProperties;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.controller.support.WxPayNotifyHandler;
import cc.ivera.handler.GlobalExceptionHandler;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.service.AliPayService;
import cc.ivera.service.CheckoutService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.wxpay.WxPayBillFacade;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.transaction.support.TransactionTemplate;

import static org.mockito.Mockito.mock;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

class OrderCreationIdempotencyHeaderMvcTest {

    @Test
    void everyOrderCreationEndpointRejectsAMissingIdempotencyHeaderWithHttp400() throws Exception {
        MockMvc mvc = MockMvcBuilders.standaloneSetup(
                        new OrderInfoController(mock(OrderInfoService.class), mock(CheckoutService.class)),
                        wxController(),
                        wxV2Controller(),
                        aliController()
                )
                .setControllerAdvice(new GlobalExceptionHandler())
                .build();

        mvc.perform(post("/api/order-info/checkout")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"paymentAppId\":9}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/wx-pay/native/9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/wx-pay-v2/native/9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
        mvc.perform(post("/api/ali-pay/trade/page/pay/9"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    private WxPayController wxController() {
        return new WxPayController(
                mock(WxPayOrderFacade.class),
                mock(WxPayRefundFacade.class),
                mock(WxPayBillFacade.class),
                mock(WxPayNotifyHandler.class),
                mock(OrderInfoService.class)
        );
    }

    private WxPayV2Controller wxV2Controller() {
        return new WxPayV2Controller(
                mock(WxPayOrderFacade.class),
                mock(WxPayConfig.class),
                mock(OrderInfoService.class),
                mock(PaymentInfoService.class),
                mock(cc.ivera.service.InventoryService.class),
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class),
                mock(StringRedisTemplate.class)
        );
    }

    private AliPayController aliController() {
        return new AliPayController(
                mock(AliPayService.class),
                mock(AlipayProperties.class),
                mock(PaymentConfigLoader.class),
                mock(OrderInfoService.class)
        );
    }
}
