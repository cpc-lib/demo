package cc.ivera.controller;

import cc.ivera.config.AlipayProperties;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.controller.support.WxPayNotifyHandler;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.AliPayService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.wxpay.WxPayBillFacade;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ExistingOrderPaymentTest {

    private final OrderInfoService orderInfoService = mock(OrderInfoService.class);
    private final AuthUser currentUser = new AuthUser(15L, "alice", UserRole.USER);

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void wxV3PaymentChecksOwnershipThenPaysExistingOrder() {
        AuthContext.setUser(currentUser);
        OrderInfo order = unpaidOrder("ORDER-WX3");
        when(orderInfoService.getOrderForUser("ORDER-WX3", currentUser)).thenReturn(order);
        WxPayOrderFacade facade = mock(WxPayOrderFacade.class);
        when(facade.nativePayOrder("ORDER-WX3")).thenReturn(Collections.singletonMap("codeUrl", "wx://code"));
        WxPayController controller = new WxPayController(
                facade,
                mock(WxPayRefundFacade.class),
                mock(WxPayBillFacade.class),
                mock(WxPayNotifyHandler.class),
                orderInfoService
        );

        assertEquals("wx://code", controller.nativePayOrder("ORDER-WX3").getData().get("codeUrl"));
        verify(orderInfoService).getOrderForUser("ORDER-WX3", currentUser);
        verify(facade).nativePayOrder("ORDER-WX3");
    }

    @Test
    void wxV2PaymentChecksOwnershipThenPaysExistingOrder() {
        AuthContext.setUser(currentUser);
        when(orderInfoService.getOrderForUser("ORDER-WX2", currentUser)).thenReturn(unpaidOrder("ORDER-WX2"));
        WxPayOrderFacade facade = mock(WxPayOrderFacade.class);
        when(facade.nativePayV2Order("ORDER-WX2", "127.0.0.1"))
                .thenReturn(Collections.singletonMap("codeUrl", "wx2://code"));
        WxPayV2Controller controller = new WxPayV2Controller(
                facade,
                mock(WxPayConfig.class),
                orderInfoService,
                mock(PaymentInfoService.class),
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class),
                mock(StringRedisTemplate.class)
        );
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");

        assertEquals("wx2://code", controller.createNativeOrder("ORDER-WX2", request).getData().get("codeUrl"));
        verify(orderInfoService).getOrderForUser("ORDER-WX2", currentUser);
        verify(facade).nativePayV2Order("ORDER-WX2", "127.0.0.1");
    }

    @Test
    void alipayChecksOwnershipThenPaysExistingOrder() {
        AuthContext.setUser(currentUser);
        when(orderInfoService.getOrderForUser("ORDER-ALI", currentUser)).thenReturn(unpaidOrder("ORDER-ALI"));
        AliPayService aliPayService = mock(AliPayService.class);
        when(aliPayService.tradeCreateOrder("ORDER-ALI")).thenReturn("<form>pay</form>");
        AliPayController controller = new AliPayController(
                aliPayService,
                mock(AlipayProperties.class),
                mock(PaymentConfigLoader.class),
                orderInfoService
        );

        assertEquals("<form>pay</form>",
                controller.tradePagePayOrder("ORDER-ALI").getData().get("formStr"));
        verify(orderInfoService).getOrderForUser("ORDER-ALI", currentUser);
        verify(aliPayService).tradeCreateOrder("ORDER-ALI");
    }

    @Test
    void cancellingOrdersChecksOwnershipBeforeCallingPaymentChannel() {
        AuthContext.setUser(currentUser);
        WxPayOrderFacade wxFacade = mock(WxPayOrderFacade.class);
        WxPayController wxController = new WxPayController(
                wxFacade,
                mock(WxPayRefundFacade.class),
                mock(WxPayBillFacade.class),
                mock(WxPayNotifyHandler.class),
                orderInfoService
        );
        AliPayService aliPayService = mock(AliPayService.class);
        AliPayController aliController = new AliPayController(
                aliPayService,
                mock(AlipayProperties.class),
                mock(PaymentConfigLoader.class),
                orderInfoService
        );

        wxController.cancel("ORDER-WX-CANCEL");
        aliController.cancel("ORDER-ALI-CANCEL");

        verify(orderInfoService).getOrderForUser("ORDER-WX-CANCEL", currentUser);
        verify(orderInfoService).getOrderForUser("ORDER-ALI-CANCEL", currentUser);
        verify(wxFacade).cancelOrder("ORDER-WX-CANCEL");
        verify(aliPayService).cancelOrder("ORDER-ALI-CANCEL");
    }

    private OrderInfo unpaidOrder(String orderNo) {
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setUserId(15L);
        order.setOrderStatus(OrderStatus.NOTPAY.getType());
        return order;
    }
}
