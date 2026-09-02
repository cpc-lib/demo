package cc.ivera.service.impl;

import cc.ivera.entity.InventoryOperation;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;
import cc.ivera.entity.Product;
import cc.ivera.enums.InventoryStatus;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.ProductStatus;
import cc.ivera.exception.ConflictException;
import cc.ivera.mapper.InventoryOperationMapper;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.InventoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InOrder;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private OrderInfoMapper orderInfoMapper;
    private OrderItemMapper orderItemMapper;
    private ProductMapper productMapper;
    private InventoryOperationMapper operationMapper;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        orderInfoMapper = mock(OrderInfoMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        productMapper = mock(ProductMapper.class);
        operationMapper = mock(InventoryOperationMapper.class);
        inventoryService = new InventoryServiceImpl(
                orderInfoMapper,
                orderItemMapper,
                productMapper,
                operationMapper
        );
    }

    @Test
    void reserveUsesConditionalAvailableStockUpdateAndWritesOneOperation() {
        OrderInfo order = order(11L, "ORDER-1", OrderStatus.NOTPAY);
        OrderItem item = item(101L, 11L, 7L, 3, InventoryStatus.RESERVED);
        when(productMapper.selectByIdForUpdate(7L))
                .thenReturn(product(7L, ProductStatus.ON_SHELF, 10, 2, 1));
        when(productMapper.reserveStock(7L, 3)).thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);

        inventoryService.reserve(order, Collections.singletonList(item));

        verify(productMapper).reserveStock(7L, 3);
        ArgumentCaptor<InventoryOperation> captor = ArgumentCaptor.forClass(InventoryOperation.class);
        verify(operationMapper).insert(captor.capture());
        InventoryOperation operation = captor.getValue();
        assertEquals("ORDER_RESERVE:ORDER-1:7", operation.getBusinessKey());
        assertEquals("ORDER_RESERVE", operation.getOperationType());
        assertEquals(-3, operation.getAvailableDelta());
        assertEquals(3, operation.getLockedDelta());
        assertEquals(0, operation.getSoldDelta());
        assertEquals(10, operation.getAvailableBefore());
        assertEquals(7, operation.getAvailableAfter());
        assertEquals(2, operation.getLockedBefore());
        assertEquals(5, operation.getLockedAfter());
    }

    @Test
    void reserveRejectsTheConditionalUpdateLoserWithoutWritingALedger() {
        OrderInfo order = order(11L, "ORDER-2", OrderStatus.NOTPAY);
        OrderItem item = item(101L, 11L, 7L, 3, InventoryStatus.RESERVED);
        when(productMapper.selectByIdForUpdate(7L))
                .thenReturn(product(7L, ProductStatus.ON_SHELF, 10, 0, 0));
        when(productMapper.reserveStock(7L, 3)).thenReturn(0);

        assertThrows(ConflictException.class,
                () -> inventoryService.reserve(order, Collections.singletonList(item)));

        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void reserveLocksProductsInAscendingIdOrder() {
        OrderInfo order = order(11L, "ORDER-SORT", OrderStatus.NOTPAY);
        OrderItem high = item(102L, 11L, 9L, 1, InventoryStatus.RESERVED);
        OrderItem low = item(101L, 11L, 7L, 1, InventoryStatus.RESERVED);
        when(productMapper.selectByIdForUpdate(7L))
                .thenReturn(product(7L, ProductStatus.ON_SHELF, 10, 0, 0));
        when(productMapper.selectByIdForUpdate(9L))
                .thenReturn(product(9L, ProductStatus.ON_SHELF, 10, 0, 0));
        when(productMapper.reserveStock(anyLong(), anyInt())).thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);

        inventoryService.reserve(order, Arrays.asList(high, low));

        InOrder inOrder = inOrder(productMapper);
        inOrder.verify(productMapper).selectByIdForUpdate(7L);
        inOrder.verify(productMapper).selectByIdForUpdate(9L);
    }

    @Test
    void repeatedReserveBusinessKeyDoesNotMoveStockAgain() {
        OrderInfo order = order(11L, "ORDER-REPLAY", OrderStatus.NOTPAY);
        OrderItem item = item(101L, 11L, 7L, 2, InventoryStatus.RESERVED);
        when(productMapper.selectByIdForUpdate(7L))
                .thenReturn(product(7L, ProductStatus.ON_SHELF, 8, 2, 0));
        InventoryOperation existing = new InventoryOperation();
        existing.setBusinessKey("ORDER_RESERVE:ORDER-REPLAY:7");
        existing.setOrderNo("ORDER-REPLAY");
        existing.setProductId(7L);
        existing.setOperationType("ORDER_RESERVE");
        existing.setAvailableDelta(-2);
        existing.setLockedDelta(2);
        existing.setSoldDelta(0);
        when(operationMapper.selectByBusinessKeyForUpdate(existing.getBusinessKey())).thenReturn(existing);

        inventoryService.reserve(order, Collections.singletonList(item));

        verify(productMapper, never()).reserveStock(anyLong(), anyInt());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void paymentCommitMovesReservedToSoldAndTransitionsEachItemOnce() {
        OrderInfo order = order(11L, "ORDER-PAID", OrderStatus.SUCCESS);
        OrderItem item = item(101L, 11L, 7L, 3, InventoryStatus.RESERVED);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-PAID")).thenReturn(order);
        when(orderItemMapper.selectByOrderIdForUpdate(11L)).thenReturn(Collections.singletonList(item));
        when(productMapper.selectByIdForUpdate(7L))
                .thenReturn(product(7L, ProductStatus.OFF_SHELF, 4, 3, 5));
        when(productMapper.commitReservedStock(7L, 3)).thenReturn(1);
        when(orderItemMapper.updateInventoryStatus(101L, InventoryStatus.RESERVED, InventoryStatus.SOLD))
                .thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);

        assertTrue(inventoryService.commitPayment("ORDER-PAID"));

        verify(productMapper).commitReservedStock(7L, 3);
        verify(orderItemMapper).updateInventoryStatus(
                101L,
                InventoryStatus.RESERVED,
                InventoryStatus.SOLD
        );
        ArgumentCaptor<InventoryOperation> captor = ArgumentCaptor.forClass(InventoryOperation.class);
        verify(operationMapper).insert(captor.capture());
        assertEquals("ORDER_COMMIT:ORDER-PAID:7", captor.getValue().getBusinessKey());
        assertEquals(0, captor.getValue().getAvailableDelta());
        assertEquals(-3, captor.getValue().getLockedDelta());
        assertEquals(3, captor.getValue().getSoldDelta());
    }

    @Test
    void repeatedPaymentCommitIsANoOp() {
        OrderInfo order = order(11L, "ORDER-PAID", OrderStatus.SUCCESS);
        OrderItem sold = item(101L, 11L, 7L, 3, InventoryStatus.SOLD);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-PAID")).thenReturn(order);
        when(orderItemMapper.selectByOrderIdForUpdate(11L)).thenReturn(Collections.singletonList(sold));

        assertFalse(inventoryService.commitPayment("ORDER-PAID"));

        verify(productMapper, never()).commitReservedStock(anyLong(), anyInt());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void mixedFinalAndReservedItemStatesAreRejectedInsteadOfPartiallyRepaired() {
        OrderInfo order = order(11L, "ORDER-MIXED", OrderStatus.SUCCESS);
        OrderItem sold = item(101L, 11L, 7L, 1, InventoryStatus.SOLD);
        OrderItem reserved = item(102L, 11L, 9L, 1, InventoryStatus.RESERVED);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-MIXED")).thenReturn(order);
        when(orderItemMapper.selectByOrderIdForUpdate(11L)).thenReturn(Arrays.asList(sold, reserved));

        assertThrows(ConflictException.class, () -> inventoryService.commitPayment("ORDER-MIXED"));

        verify(productMapper, never()).commitReservedStock(anyLong(), anyInt());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void confirmedCloseReleasesReservedStockAndRepeatedReleaseIsANoOp() {
        OrderInfo order = order(11L, "ORDER-CLOSED", OrderStatus.CLOSED);
        OrderItem reserved = item(101L, 11L, 7L, 2, InventoryStatus.RESERVED);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-CLOSED")).thenReturn(order);
        when(orderItemMapper.selectByOrderIdForUpdate(11L))
                .thenReturn(Collections.singletonList(reserved))
                .thenReturn(Collections.singletonList(item(101L, 11L, 7L, 2, InventoryStatus.RELEASED)));
        when(productMapper.selectByIdForUpdate(7L))
                .thenReturn(product(7L, ProductStatus.ON_SHELF, 4, 2, 5));
        when(productMapper.releaseReservedStock(7L, 2)).thenReturn(1);
        when(orderItemMapper.updateInventoryStatus(101L, InventoryStatus.RESERVED, InventoryStatus.RELEASED))
                .thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);

        assertTrue(inventoryService.releaseReservation("ORDER-CLOSED"));
        assertFalse(inventoryService.releaseReservation("ORDER-CLOSED"));

        verify(productMapper, times(1)).releaseReservedStock(7L, 2);
        verify(operationMapper, times(1)).insert(any(InventoryOperation.class));
    }

    @Test
    void unknownOrStillUnpaidStateKeepsInventoryLocked() {
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-UNKNOWN"))
                .thenReturn(order(11L, "ORDER-UNKNOWN", OrderStatus.NOTPAY));

        assertFalse(inventoryService.releaseReservation("ORDER-UNKNOWN"));

        verify(orderItemMapper, never()).selectByOrderIdForUpdate(anyLong());
        verify(productMapper, never()).releaseReservedStock(anyLong(), anyInt());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    private OrderInfo order(Long id, String orderNo, OrderStatus status) {
        OrderInfo order = new OrderInfo();
        order.setId(id);
        order.setOrderNo(orderNo);
        order.setOrderStatus(status.getType());
        return order;
    }

    private OrderItem item(
            Long id,
            Long orderId,
            Long productId,
            int quantity,
            InventoryStatus status
    ) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        item.setInventoryStatus(status);
        item.setRefundedQuantity(0);
        return item;
    }

    private Product product(
            Long id,
            ProductStatus status,
            int available,
            int locked,
            int sold
    ) {
        Product product = new Product();
        product.setId(id);
        product.setStatus(status);
        product.setAvailableStock(available);
        product.setLockedStock(locked);
        product.setSoldStock(sold);
        return product;
    }
}
