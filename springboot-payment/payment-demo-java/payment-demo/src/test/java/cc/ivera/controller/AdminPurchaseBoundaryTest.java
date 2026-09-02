package cc.ivera.controller;

import cc.ivera.config.AlipayProperties;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.WxPayConfig;
import cc.ivera.controller.support.WxPayNotifyHandler;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
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

import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class AdminPurchaseBoundaryTest {

    private static final AuthUser ADMIN = new AuthUser(1L, "admin", UserRole.ADMIN);

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void adminCannotStartWxV3ProductPayment() {
        AuthContext.setUser(ADMIN);
        WxPayOrderFacade facade = mock(WxPayOrderFacade.class);
        WxPayController controller = wxController(facade, mock(OrderInfoService.class));

        assertThrows(ForbiddenException.class, () -> controller.nativePay(9L, null, "key-1"));

        verifyNoInteractions(facade);
    }

    @Test
    void adminCannotPayExistingWxV3Order() {
        AuthContext.setUser(ADMIN);
        WxPayOrderFacade facade = mock(WxPayOrderFacade.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayController controller = wxController(facade, orderInfoService);

        assertThrows(ForbiddenException.class, () -> controller.nativePayOrder("ORDER-1"));

        verifyNoInteractions(facade, orderInfoService);
    }

    @Test
    void adminCannotStartWxV2ProductPayment() {
        AuthContext.setUser(ADMIN);
        WxPayOrderFacade facade = mock(WxPayOrderFacade.class);
        WxPayV2Controller controller = wxV2Controller(facade, mock(OrderInfoService.class));
        MockHttpServletRequest request = request();

        assertThrows(ForbiddenException.class,
                () -> controller.createNative(9L, null, "key-1", request));

        verifyNoInteractions(facade);
    }

    @Test
    void adminCannotPayExistingWxV2Order() {
        AuthContext.setUser(ADMIN);
        WxPayOrderFacade facade = mock(WxPayOrderFacade.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayV2Controller controller = wxV2Controller(facade, orderInfoService);

        assertThrows(ForbiddenException.class,
                () -> controller.createNativeOrder("ORDER-1", request()));

        verifyNoInteractions(facade, orderInfoService);
    }

    @Test
    void adminCannotStartAlipayProductPayment() {
        AuthContext.setUser(ADMIN);
        AliPayService service = mock(AliPayService.class);
        AliPayController controller = aliController(service, mock(OrderInfoService.class));

        assertThrows(ForbiddenException.class,
                () -> controller.tradePagePay(9L, null, "key-1"));

        verifyNoInteractions(service);
    }

    @Test
    void adminCannotPayExistingAlipayOrder() {
        AuthContext.setUser(ADMIN);
        AliPayService service = mock(AliPayService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        AliPayController controller = aliController(service, orderInfoService);

        assertThrows(ForbiddenException.class, () -> controller.tradePagePayOrder("ORDER-1"));

        verifyNoInteractions(service, orderInfoService);
    }

    @Test
    void userWxV3DirectPurchasePassesBackendKeyUnchanged() {
        AuthContext.setUser(new AuthUser(7L, "alice", UserRole.USER));
        WxPayOrderFacade facade = mock(WxPayOrderFacade.class);
        WxPayController controller = wxController(facade, mock(OrderInfoService.class));
        Map<String, Object> result = java.util.Collections.singletonMap("orderNo", "ORDER-1");
        when(facade.nativePay(9L, null, "key-1")).thenReturn(result);

        assertEquals(result, controller.nativePay(9L, null, "key-1").getData());
        verify(facade).nativePay(9L, null, "key-1");
    }

    @Test
    void userWxV2DirectPurchasePassesBackendKeyUnchanged() {
        AuthContext.setUser(new AuthUser(7L, "alice", UserRole.USER));
        WxPayOrderFacade facade = mock(WxPayOrderFacade.class);
        WxPayV2Controller controller = wxV2Controller(facade, mock(OrderInfoService.class));
        Map<String, Object> result = java.util.Collections.singletonMap("orderNo", "ORDER-1");
        when(facade.nativePayV2(9L, "127.0.0.1", null, "key-1")).thenReturn(result);

        assertEquals(result, controller.createNative(9L, null, "key-1", request()).getData());
        verify(facade).nativePayV2(9L, "127.0.0.1", null, "key-1");
    }

    @Test
    void userAlipayDirectPurchasePassesBackendKeyUnchanged() {
        AuthContext.setUser(new AuthUser(7L, "alice", UserRole.USER));
        AliPayService service = mock(AliPayService.class);
        AliPayController controller = aliController(service, mock(OrderInfoService.class));
        when(service.tradeCreate(9L, null, "key-1")).thenReturn("form");

        assertEquals("form", controller.tradePagePay(9L, null, "key-1").getData().get("formStr"));
        verify(service).tradeCreate(9L, null, "key-1");
    }

    private WxPayController wxController(WxPayOrderFacade facade, OrderInfoService orderInfoService) {
        return new WxPayController(
                facade,
                mock(WxPayRefundFacade.class),
                mock(WxPayBillFacade.class),
                mock(WxPayNotifyHandler.class),
                orderInfoService
        );
    }

    private WxPayV2Controller wxV2Controller(WxPayOrderFacade facade, OrderInfoService orderInfoService) {
        return new WxPayV2Controller(
                facade,
                mock(WxPayConfig.class),
                orderInfoService,
                mock(PaymentInfoService.class),
                mock(cc.ivera.service.InventoryService.class),
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class),
                mock(StringRedisTemplate.class)
        );
    }

    private AliPayController aliController(AliPayService service, OrderInfoService orderInfoService) {
        return new AliPayController(
                service,
                mock(AlipayProperties.class),
                mock(PaymentConfigLoader.class),
                orderInfoService
        );
    }

    private MockHttpServletRequest request() {
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setRemoteAddr("127.0.0.1");
        return request;
    }
}
