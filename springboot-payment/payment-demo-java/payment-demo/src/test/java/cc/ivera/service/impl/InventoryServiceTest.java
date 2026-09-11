package cc.ivera.service.impl;

import cc.ivera.entity.InventoryOperation;
import cc.ivera.entity.InventoryReservation;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;
import cc.ivera.entity.Product;
import cc.ivera.entity.ProductSku;
import cc.ivera.entity.SkuStock;
import cc.ivera.enums.InventoryStatus;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.ProductStatus;
import cc.ivera.enums.ReservationStatus;
import cc.ivera.exception.ConflictException;
import cc.ivera.mapper.InventoryOperationMapper;
import cc.ivera.mapper.InventoryReservationMapper;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.mapper.ProductSkuMapper;
import cc.ivera.mapper.SkuStockMapper;
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
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class InventoryServiceTest {

    private OrderInfoMapper orderInfoMapper;
    private OrderItemMapper orderItemMapper;
    private ProductMapper productMapper;
    private ProductSkuMapper productSkuMapper;
    private SkuStockMapper skuStockMapper;
    private InventoryReservationMapper reservationMapper;
    private InventoryOperationMapper operationMapper;
    private InventoryService inventoryService;

    @BeforeEach
    void setUp() {
        orderInfoMapper = mock(OrderInfoMapper.class);
        orderItemMapper = mock(OrderItemMapper.class);
        productMapper = mock(ProductMapper.class);
        productSkuMapper = mock(ProductSkuMapper.class);
        skuStockMapper = mock(SkuStockMapper.class);
        reservationMapper = mock(InventoryReservationMapper.class);
        operationMapper = mock(InventoryOperationMapper.class);
        inventoryService = new InventoryServiceImpl(
                orderInfoMapper,
                orderItemMapper,
                productMapper,
                productSkuMapper,
                skuStockMapper,
                reservationMapper,
                operationMapper
        );
    }

    @Test
    void reserveUsesConditionalSkuStockUpdateAndWritesReservationLedger() {
        OrderInfo order = order(11L, "ORDER-1", OrderStatus.NOTPAY);
        OrderItem item = item(101L, 11L, 7L, 70L, 3, InventoryStatus.RESERVED);
        stubProductAndSku(7L, 70L, ProductStatus.ON_SHELF);
        when(skuStockMapper.selectBySkuIdForUpdate(70L)).thenReturn(stock(70L, 10, 2));
        when(skuStockMapper.reserveStock(70L, 3)).thenReturn(1);
        when(reservationMapper.insert(any(InventoryReservation.class))).thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);

        inventoryService.reserve(order, Collections.singletonList(item));

        verify(skuStockMapper).reserveStock(70L, 3);
        ArgumentCaptor<InventoryOperation> captor = ArgumentCaptor.forClass(InventoryOperation.class);
        verify(operationMapper).insert(captor.capture());
        InventoryOperation operation = captor.getValue();
        assertTrue(operation.getBusinessKey().startsWith("ORDER_RESERVE:RSV"));
        assertEquals(70L, operation.getSkuId());
        assertEquals(-3, operation.getAvailableDelta());
        assertEquals(3, operation.getLockedDelta());
        assertEquals(0, operation.getSoldDelta());
        assertEquals(10, operation.getAvailableBefore());
        assertEquals(7, operation.getAvailableAfter());
        assertEquals(2, operation.getLockedBefore());
        assertEquals(5, operation.getLockedAfter());
    }

    @Test
    void reserveRejectsConditionalSkuUpdateLoserWithoutWritingLedger() {
        OrderInfo order = order(11L, "ORDER-2", OrderStatus.NOTPAY);
        OrderItem item = item(101L, 11L, 7L, 70L, 3, InventoryStatus.RESERVED);
        stubProductAndSku(7L, 70L, ProductStatus.ON_SHELF);
        when(skuStockMapper.selectBySkuIdForUpdate(70L)).thenReturn(stock(70L, 10, 0));
        when(skuStockMapper.reserveStock(70L, 3)).thenReturn(0);
        when(reservationMapper.insert(any(InventoryReservation.class))).thenReturn(1);

        assertThrows(ConflictException.class,
                () -> inventoryService.reserve(order, Collections.singletonList(item)));

        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void reserveLocksProductsAndSkuRowsInAscendingOrder() {
        OrderInfo order = order(11L, "ORDER-SORT", OrderStatus.NOTPAY);
        OrderItem high = item(102L, 11L, 9L, 90L, 1, InventoryStatus.RESERVED);
        OrderItem low = item(101L, 11L, 7L, 70L, 1, InventoryStatus.RESERVED);
        stubProductAndSku(7L, 70L, ProductStatus.ON_SHELF);
        stubProductAndSku(9L, 90L, ProductStatus.ON_SHELF);
        when(skuStockMapper.selectBySkuIdForUpdate(70L)).thenReturn(stock(70L, 10, 0));
        when(skuStockMapper.selectBySkuIdForUpdate(90L)).thenReturn(stock(90L, 10, 0));
        when(skuStockMapper.reserveStock(anyLong(), anyInt())).thenReturn(1);
        when(reservationMapper.insert(any(InventoryReservation.class))).thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);

        inventoryService.reserve(order, Arrays.asList(high, low));

        InOrder productOrder = inOrder(productMapper);
        productOrder.verify(productMapper).selectByIdForUpdate(7L);
        productOrder.verify(productMapper).selectByIdForUpdate(9L);
        InOrder stockOrder = inOrder(skuStockMapper);
        stockOrder.verify(skuStockMapper).selectBySkuIdForUpdate(70L);
        stockOrder.verify(skuStockMapper).selectBySkuIdForUpdate(90L);
    }

    @Test
    void mixedCommittedAndLockedReservationsAreRejectedWithoutStockMutation() {
        OrderInfo order = order(11L, "ORDER-MIXED", OrderStatus.SUCCESS);
        OrderItem first = item(101L, 11L, 7L, 70L, 1, InventoryStatus.SOLD);
        OrderItem second = item(102L, 11L, 9L, 90L, 1, InventoryStatus.RESERVED);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-MIXED")).thenReturn(order);
        when(orderItemMapper.selectByOrderIdForUpdate(11L)).thenReturn(Arrays.asList(first, second));
        when(reservationMapper.selectByOrderNoForUpdate("ORDER-MIXED")).thenReturn(Arrays.asList(
                reservation(201L, "RSV-1", order, first, ReservationStatus.COMMITTED),
                reservation(202L, "RSV-2", order, second, ReservationStatus.LOCKED)
        ));

        assertThrows(ConflictException.class, () -> inventoryService.commitPayment("ORDER-MIXED"));

        verify(skuStockMapper, never()).commitLockedStock(anyLong(), anyInt());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void exactCommittedReservationReplayIsANoOp() {
        OrderInfo order = order(11L, "ORDER-PAID", OrderStatus.SUCCESS);
        OrderItem item = item(101L, 11L, 7L, 70L, 3, InventoryStatus.SOLD);
        InventoryReservation committed = reservation(
                201L, "RSV-PAID", order, item, ReservationStatus.COMMITTED);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-PAID")).thenReturn(order);
        when(orderItemMapper.selectByOrderIdForUpdate(11L)).thenReturn(Collections.singletonList(item));
        when(reservationMapper.selectByOrderNoForUpdate("ORDER-PAID"))
                .thenReturn(Collections.singletonList(committed));
        when(operationMapper.selectByBusinessKeyForUpdate("ORDER_COMMIT:RSV-PAID"))
                .thenReturn(operation("ORDER_COMMIT:RSV-PAID", "ORDER_COMMIT", order, item, 0, -3));

        assertFalse(inventoryService.commitPayment("ORDER-PAID"));

        verify(skuStockMapper, never()).commitLockedStock(anyLong(), anyInt());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void unpaidOrderKeepsReservationLocked() {
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-UNPAID"))
                .thenReturn(order(11L, "ORDER-UNPAID", OrderStatus.NOTPAY));

        assertFalse(inventoryService.releaseReservation("ORDER-UNPAID"));

        verify(orderItemMapper, never()).selectByOrderIdForUpdate(anyLong());
        verify(skuStockMapper, never()).releaseLockedStock(anyLong(), anyInt());
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    private void stubProductAndSku(Long productId, Long skuId, ProductStatus status) {
        Product product = new Product();
        product.setId(productId);
        product.setStatus(status);
        when(productMapper.selectByIdForUpdate(productId)).thenReturn(product);

        ProductSku sku = new ProductSku();
        sku.setId(skuId);
        sku.setProductId(productId);
        sku.setStatus("ACTIVE");
        when(productSkuMapper.selectById(skuId)).thenReturn(sku);
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
            Long skuId,
            int quantity,
            InventoryStatus status
    ) {
        OrderItem item = new OrderItem();
        item.setId(id);
        item.setOrderId(orderId);
        item.setProductId(productId);
        item.setSkuId(skuId);
        item.setQuantity(quantity);
        item.setInventoryStatus(status);
        return item;
    }

    private SkuStock stock(Long skuId, int available, int locked) {
        SkuStock stock = new SkuStock();
        stock.setSkuId(skuId);
        stock.setAvailableStock(available);
        stock.setLockedStock(locked);
        stock.setVersion(0L);
        return stock;
    }

    private InventoryReservation reservation(
            Long id,
            String reservationNo,
            OrderInfo order,
            OrderItem item,
            ReservationStatus status
    ) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(id);
        reservation.setReservationNo(reservationNo);
        reservation.setOrderNo(order.getOrderNo());
        reservation.setOrderItemId(item.getId());
        reservation.setSkuId(item.getSkuId());
        reservation.setQuantity(item.getQuantity());
        reservation.setStatus(status);
        return reservation;
    }

    private InventoryOperation operation(
            String key,
            String type,
            OrderInfo order,
            OrderItem item,
            int availableDelta,
            int lockedDelta
    ) {
        InventoryOperation operation = new InventoryOperation();
        operation.setBusinessKey(key);
        operation.setOperationType(type);
        operation.setOrderNo(order.getOrderNo());
        operation.setProductId(item.getProductId());
        operation.setSkuId(item.getSkuId());
        operation.setOrderItemId(item.getId());
        operation.setAvailableDelta(availableDelta);
        operation.setLockedDelta(lockedDelta);
        operation.setSoldDelta(0);
        return operation;
    }
}
