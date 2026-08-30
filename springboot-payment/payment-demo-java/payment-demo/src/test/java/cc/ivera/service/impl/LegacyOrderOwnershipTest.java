package cc.ivera.service.impl;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.Product;
import cc.ivera.enums.UserRole;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.OrderCloseMessageService;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.TransactionCallback;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.function.Supplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class LegacyOrderOwnershipTest {

    @AfterEach
    void clearContext() {
        AuthContext.clear();
    }

    @Test
    @SuppressWarnings("unchecked")
    void directPurchaseScopesReuseAndNewOrderToAuthenticatedUser() {
        AuthContext.setUser(new AuthUser(55L, "alice", UserRole.USER));
        ProductMapper productMapper = mock(ProductMapper.class);
        OrderInfoMapper orderInfoMapper = mock(OrderInfoMapper.class);
        DistributedLockTemplate lockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        when(lockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> ((Supplier<?>) invocation.getArgument(3)).get());
        when(transactionTemplate.execute(any(TransactionCallback.class))).thenAnswer(invocation -> {
            TransactionCallback<?> callback = invocation.getArgument(0);
            return callback.doInTransaction(mock(TransactionStatus.class));
        });
        Product product = new Product();
        product.setId(101L);
        product.setTitle("Java");
        product.setPrice(1000);
        when(productMapper.selectById(101L)).thenReturn(product);

        OrderInfoServiceImpl service = new OrderInfoServiceImpl(
                productMapper,
                mock(OrderCloseMessageService.class),
                lockTemplate,
                transactionTemplate,
                mock(OrderItemMapper.class)
        );
        ReflectionTestUtils.setField(service, "baseMapper", orderInfoMapper);

        service.createOrReuseOrder(101L, "微信", 9L, "WXPAY");

        verify(orderInfoMapper).selectNoPayOrderForUpdate(101L, "微信", "未支付", 9L, 55L);
        ArgumentCaptor<OrderInfo> orderCaptor = ArgumentCaptor.forClass(OrderInfo.class);
        verify(orderInfoMapper).insert(orderCaptor.capture());
        assertEquals(55L, orderCaptor.getValue().getUserId());
    }
}
