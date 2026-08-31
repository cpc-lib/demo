package cc.ivera.controller;

import cc.ivera.dto.RefundRequest;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.RefundApplicationService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;

class RefundOwnershipTest {

    private final RefundApplicationService refundService = mock(RefundApplicationService.class);
    private final OrderInfoService orderInfoService = mock(OrderInfoService.class);
    private final AuthUser user = new AuthUser(71L, "alice", UserRole.USER);

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void refundApplicationChecksOrderOwnershipBeforeCreatingApplication() {
        AuthContext.setUser(user);
        RefundApplicationController controller = new RefundApplicationController(refundService, orderInfoService);
        RefundRequest request = new RefundRequest();
        request.setOrderNo("ORDER-71");
        request.setRefundAmount(500);
        request.setReason("不需要了");

        controller.apply(request);

        verify(orderInfoService).getOrderForUser("ORDER-71", user);
        verify(refundService).createApplication("ORDER-71", 500, "不需要了");
    }

    @Test
    void adminCannotApplyForRefund() {
        AuthContext.setUser(new AuthUser(1L, "admin", UserRole.ADMIN));
        RefundApplicationController controller = new RefundApplicationController(refundService, orderInfoService);
        RefundRequest request = new RefundRequest();
        request.setOrderNo("ORDER-ADMIN");
        request.setRefundAmount(500);
        request.setReason("不需要了");

        assertThrows(ForbiddenException.class, () -> controller.apply(request));
    }

    @Test
    void personalRefundListChecksOrderOwnership() {
        AuthContext.setUser(user);
        RefundInfoController controller = new RefundInfoController(refundService, orderInfoService);

        controller.listByOrderNo("ORDER-71");

        verify(orderInfoService).getOrderForUser("ORDER-71", user);
        verify(refundService).listByOrderNo("ORDER-71");
    }
}
