package cc.ivera.controller;

import cc.ivera.dto.CheckoutRequest;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.CheckoutService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.vo.CheckoutResult;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class OrderOwnershipTest {

    private final OrderInfoService orderInfoService = mock(OrderInfoService.class);
    private final CheckoutService checkoutService = mock(CheckoutService.class);
    private final OrderInfoController controller = new OrderInfoController(orderInfoService, checkoutService);

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void checkoutAndPersonalQueriesUseAuthenticatedUserOnly() {
        AuthUser user = new AuthUser(42L, "alice", UserRole.USER);
        AuthContext.setUser(user);
        CheckoutRequest request = new CheckoutRequest(9L, "request-42");
        CheckoutResult checkoutResult = new CheckoutResult("ORDER-42", 5000, 9L, "WXPAY", "未支付");
        when(checkoutService.checkout(42L, 9L, "request-42")).thenReturn(checkoutResult);
        OrderInfo order = new OrderInfo();
        order.setOrderNo("ORDER-42");
        when(orderInfoService.listOrderByUserId(42L)).thenReturn(Collections.singletonList(order));
        OrderItem item = new OrderItem();
        item.setProductTitle("Java");
        when(orderInfoService.listOrderItemsForUser("ORDER-42", user)).thenReturn(Arrays.asList(item));

        assertEquals(checkoutResult, controller.checkout(request).getData());
        assertEquals(order, ((java.util.List<?>) controller.myList().getData().get("list")).get(0));
        assertEquals(item, controller.orderItems("ORDER-42").getData().get(0));

        verify(checkoutService).checkout(42L, 9L, "request-42");
        verify(orderInfoService).listOrderByUserId(42L);
        verify(orderInfoService).listOrderItemsForUser("ORDER-42", user);
    }

    @Test
    void orderStatusDelegatesWithCurrentUserForOwnershipCheck() {
        AuthUser user = new AuthUser(43L, "bob", UserRole.USER);
        AuthContext.setUser(user);
        when(orderInfoService.getOrderStatusForUser("ORDER-43", user)).thenReturn("支付成功");

        controller.queryOrderStatus("ORDER-43");

        verify(orderInfoService).getOrderStatusForUser("ORDER-43", user);
    }

    @Test
    void adminCannotCheckout() {
        AuthContext.setUser(new AuthUser(1L, "admin", UserRole.ADMIN));

        assertThrows(ForbiddenException.class,
                () -> controller.checkout(new CheckoutRequest(9L, "admin-checkout")));
    }
}
