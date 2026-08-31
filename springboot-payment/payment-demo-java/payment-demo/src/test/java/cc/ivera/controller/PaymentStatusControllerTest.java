package cc.ivera.controller;

import cc.ivera.config.AlipayProperties;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.exception.UnauthorizedException;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.AliPayService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.wxpay.WxPayBillFacade;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import cc.ivera.controller.support.WxPayNotifyHandler;
import cc.ivera.vo.R;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class PaymentStatusControllerTest {

    private final OrderInfoService orderInfoService = mock(OrderInfoService.class);

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void adminCanQueryAlipayStatusAndReceivesStableLocalStatus() {
        AuthUser admin = new AuthUser(1L, "admin", UserRole.ADMIN);
        AuthContext.setUser(admin);
        when(orderInfoService.getOrderForUser("ORDER-ALI", admin)).thenReturn(order("ORDER-ALI"));
        when(orderInfoService.getOrderStatus("ORDER-ALI")).thenReturn(OrderStatus.SUCCESS.getType());
        AliPayService aliPayService = mock(AliPayService.class);
        AliPayController controller = aliController(aliPayService);

        R<Map<String, Object>> response = controller.checkOrderStatus("ORDER-ALI");
        assertEquals("ORDER-ALI", response.getData().get("orderNo"));
        assertEquals(OrderStatus.SUCCESS.getType(), response.getData().get("orderStatus"));
        assertEquals("查询成功", response.getMessage());
        verify(orderInfoService).getOrderForUser("ORDER-ALI", admin);
        verify(aliPayService).checkOrderStatus("ORDER-ALI");
    }

    @Test
    void userStatusQueryStillUsesOrderOwnershipRule() {
        AuthUser user = new AuthUser(15L, "alice", UserRole.USER);
        AuthContext.setUser(user);
        when(orderInfoService.getOrderForUser("ORDER-ALI", user)).thenReturn(order("ORDER-ALI"));
        when(orderInfoService.getOrderStatus("ORDER-ALI")).thenReturn(OrderStatus.NOTPAY.getType());
        AliPayService aliPayService = mock(AliPayService.class);
        AliPayController controller = aliController(aliPayService);

        controller.checkOrderStatus("ORDER-ALI");

        verify(orderInfoService).getOrderForUser("ORDER-ALI", user);
        verify(aliPayService).checkOrderStatus("ORDER-ALI");
    }

    @Test
    void userCannotQueryAnotherUsersOrder() {
        AuthUser user = new AuthUser(15L, "alice", UserRole.USER);
        AuthContext.setUser(user);
        when(orderInfoService.getOrderForUser("ORDER-OTHER", user))
                .thenThrow(new ForbiddenException("无权访问该订单"));
        AliPayService aliPayService = mock(AliPayService.class);
        AliPayController controller = aliController(aliPayService);

        assertThrows(ForbiddenException.class, () -> controller.checkOrderStatus("ORDER-OTHER"));
        verifyNoInteractions(aliPayService);
    }

    @Test
    void unauthenticatedStatusQueryReturnsUnauthorized() {
        AliPayService aliPayService = mock(AliPayService.class);
        AliPayController controller = aliController(aliPayService);

        assertThrows(UnauthorizedException.class, () -> controller.checkOrderStatus("ORDER-ALI"));
        verifyNoInteractions(aliPayService);
    }

    @Test
    void wxStatusQueryAlsoChecksOrderOwnershipBeforeChannelCall() {
        AuthUser user = new AuthUser(15L, "alice", UserRole.USER);
        AuthContext.setUser(user);
        when(orderInfoService.getOrderForUser("ORDER-WX", user)).thenReturn(order("ORDER-WX"));
        WxPayOrderFacade wxPayOrderFacade = mock(WxPayOrderFacade.class);
        when(wxPayOrderFacade.queryPaymentStatus("ORDER-WX"))
                .thenReturn(Collections.singletonMap("orderNo", "ORDER-WX"));
        WxPayController controller = new WxPayController(
                wxPayOrderFacade,
                mock(WxPayRefundFacade.class),
                mock(WxPayBillFacade.class),
                mock(WxPayNotifyHandler.class),
                orderInfoService
        );

        assertEquals("ORDER-WX", controller.checkOrderStatus("ORDER-WX").getData().get("orderNo"));
        verify(orderInfoService).getOrderForUser("ORDER-WX", user);
        verify(wxPayOrderFacade).queryPaymentStatus("ORDER-WX");
    }

    private AliPayController aliController(AliPayService aliPayService) {
        return new AliPayController(
                aliPayService,
                mock(AlipayProperties.class),
                mock(PaymentConfigLoader.class),
                orderInfoService
        );
    }

    private OrderInfo order(String orderNo) {
        OrderInfo order = new OrderInfo();
        order.setOrderNo(orderNo);
        order.setOrderStatus(OrderStatus.NOTPAY.getType());
        return order;
    }
}
