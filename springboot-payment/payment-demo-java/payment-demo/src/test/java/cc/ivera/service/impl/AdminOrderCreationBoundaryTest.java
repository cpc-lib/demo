package cc.ivera.service.impl;

import cc.ivera.enums.UserRole;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.OrderCloseMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.transaction.support.TransactionTemplate;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verifyNoInteractions;

class AdminOrderCreationBoundaryTest {

    private final ProductMapper productMapper = mock(ProductMapper.class);
    private final OrderInfoMapper orderInfoMapper = mock(OrderInfoMapper.class);
    private final DistributedLockTemplate lockTemplate = mock(DistributedLockTemplate.class);
    private final TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    void adminCannotCreateOrReuseOrderBeforeAcquiringLock() {
        AuthContext.setUser(new AuthUser(1L, "admin", UserRole.ADMIN));
        OrderInfoServiceImpl service = new OrderInfoServiceImpl(
                productMapper,
                mock(OrderCloseMessageService.class),
                lockTemplate,
                transactionTemplate,
                mock(OrderItemMapper.class)
        );

        assertThrows(ForbiddenException.class,
                () -> service.createOrReuseOrder(9L, "微信", 1L, "WXPAY"));

        verifyNoInteractions(productMapper, orderInfoMapper, lockTemplate, transactionTemplate);
    }
}
