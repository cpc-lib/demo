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
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

class InventoryReservationTest {

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
    void reserveCreatesOneReservationPerItemAndOnlyMovesSkuStock() {
        OrderInfo order = order(11L, "ORDER-1", OrderStatus.NOTPAY);
        OrderItem item = item(101L, 11L, 7L, 70L, 3, InventoryStatus.RESERVED);
        stubProductAndSku(7L, 70L, ProductStatus.ON_SHELF);
        when(skuStockMapper.selectBySkuIdForUpdate(70L)).thenReturn(stock(70L, 10, 2));
        when(skuStockMapper.reserveStock(70L, 3)).thenReturn(1);
        when(reservationMapper.insert(any(InventoryReservation.class))).thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);

        inventoryService.reserve(order, Collections.singletonList(item));

        verify(skuStockMapper).reserveStock(70L, 3);
        ArgumentCaptor<InventoryReservation> reservationCaptor =
                ArgumentCaptor.forClass(InventoryReservation.class);
        verify(reservationMapper).insert(reservationCaptor.capture());
        InventoryReservation reservation = reservationCaptor.getValue();
        assertNotNull(reservation.getReservationNo());
        assertEquals("ORDER-1", reservation.getOrderNo());
        assertEquals(101L, reservation.getOrderItemId());
        assertEquals(70L, reservation.getSkuId());
        assertEquals(3, reservation.getQuantity());
        assertEquals(ReservationStatus.LOCKED, reservation.getStatus());

        ArgumentCaptor<InventoryOperation> operationCaptor =
                ArgumentCaptor.forClass(InventoryOperation.class);
        verify(operationMapper).insert(operationCaptor.capture());
        InventoryOperation operation = operationCaptor.getValue();
        assertEquals("ORDER_RESERVE:" + reservation.getReservationNo(), operation.getBusinessKey());
        assertEquals(70L, operation.getSkuId());
        assertEquals(101L, operation.getOrderItemId());
        assertEquals(-3, operation.getAvailableDelta());
        assertEquals(3, operation.getLockedDelta());
        assertEquals(0, operation.getSoldDelta());
    }

    @Test
    void reservePreflightsEverySkuBeforeChangingAnyBalance() {
        OrderInfo order = order(11L, "ORDER-ALL-OR-NOTHING", OrderStatus.NOTPAY);
        OrderItem first = item(101L, 11L, 7L, 70L, 2, InventoryStatus.RESERVED);
        OrderItem second = item(102L, 11L, 8L, 80L, 4, InventoryStatus.RESERVED);
        stubProductAndSku(7L, 70L, ProductStatus.ON_SHELF);
        stubProductAndSku(8L, 80L, ProductStatus.ON_SHELF);
        when(skuStockMapper.selectBySkuIdForUpdate(70L)).thenReturn(stock(70L, 10, 0));
        when(skuStockMapper.selectBySkuIdForUpdate(80L)).thenReturn(stock(80L, 3, 0));

        assertThrows(ConflictException.class,
                () -> inventoryService.reserve(order, Arrays.asList(second, first)));

        verify(skuStockMapper, never()).reserveStock(anyLong(), any());
        verify(reservationMapper, never()).insert(any(InventoryReservation.class));
        verify(operationMapper, never()).insert(any(InventoryOperation.class));
    }

    @Test
    void skuRowsAreLockedInAscendingOrder() {
        OrderInfo order = order(11L, "ORDER-SORT", OrderStatus.NOTPAY);
        OrderItem high = item(102L, 11L, 9L, 90L, 1, InventoryStatus.RESERVED);
        OrderItem low = item(101L, 11L, 7L, 70L, 1, InventoryStatus.RESERVED);
        stubProductAndSku(7L, 70L, ProductStatus.ON_SHELF);
        stubProductAndSku(9L, 90L, ProductStatus.ON_SHELF);
        when(skuStockMapper.selectBySkuIdForUpdate(70L)).thenReturn(stock(70L, 10, 0));
        when(skuStockMapper.selectBySkuIdForUpdate(90L)).thenReturn(stock(90L, 10, 0));
        when(skuStockMapper.reserveStock(anyLong(), any())).thenReturn(1);
        when(reservationMapper.insert(any(InventoryReservation.class))).thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);

        inventoryService.reserve(order, Arrays.asList(high, low));

        InOrder inOrder = inOrder(skuStockMapper);
        inOrder.verify(skuStockMapper).selectBySkuIdForUpdate(70L);
        inOrder.verify(skuStockMapper).selectBySkuIdForUpdate(90L);
    }

    @Test
    void exactReservationReplayDoesNotMoveStockAgainButPartialReplayIsRejected() {
        OrderInfo order = order(11L, "ORDER-REPLAY", OrderStatus.NOTPAY);
        OrderItem first = item(101L, 11L, 7L, 70L, 2, InventoryStatus.RESERVED);
        InventoryReservation existing = reservation(
                201L, "RSV-1", "ORDER-REPLAY", first, ReservationStatus.LOCKED);
        when(reservationMapper.selectByOrderItemIdForUpdate(101L)).thenReturn(existing);
        when(operationMapper.selectByBusinessKeyForUpdate("ORDER_RESERVE:RSV-1"))
                .thenReturn(operation("ORDER_RESERVE:RSV-1", "ORDER-REPLAY", first, -2, 2));
        stubProductAndSku(7L, 70L, ProductStatus.ON_SHELF);

        inventoryService.reserve(order, Collections.singletonList(first));

        verify(skuStockMapper, never()).selectBySkuIdForUpdate(anyLong());
        verify(skuStockMapper, never()).reserveStock(anyLong(), any());

        OrderItem missing = item(102L, 11L, 8L, 80L, 1, InventoryStatus.RESERVED);
        when(reservationMapper.selectByOrderItemIdForUpdate(102L)).thenReturn(null);
        assertThrows(ConflictException.class,
                () -> inventoryService.reserve(order, Arrays.asList(first, missing)));
    }

    @Test
    void paymentCommitMovesOnlyLockedSkuBalanceAndCommitsReservationOnce() {
        OrderInfo order = order(11L, "ORDER-PAID", OrderStatus.SUCCESS);
        OrderItem item = item(101L, 11L, 7L, 70L, 3, InventoryStatus.RESERVED);
        InventoryReservation reservation = reservation(
                201L, "RSV-PAID", "ORDER-PAID", item, ReservationStatus.LOCKED);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-PAID")).thenReturn(order);
        when(orderItemMapper.selectByOrderIdForUpdate(11L)).thenReturn(Collections.singletonList(item));
        when(reservationMapper.selectByOrderNoForUpdate("ORDER-PAID"))
                .thenReturn(Collections.singletonList(reservation));
        when(skuStockMapper.selectBySkuIdForUpdate(70L)).thenReturn(stock(70L, 4, 3));
        when(skuStockMapper.commitLockedStock(70L, 3)).thenReturn(1);
        when(reservationMapper.updateStatus(201L, ReservationStatus.LOCKED, ReservationStatus.COMMITTED))
                .thenReturn(1);
        when(orderItemMapper.updateInventoryStatus(101L, InventoryStatus.RESERVED, InventoryStatus.SOLD))
                .thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);

        assertTrue(inventoryService.commitPayment("ORDER-PAID"));

        verify(skuStockMapper).commitLockedStock(70L, 3);
        verify(reservationMapper).updateStatus(
                201L, ReservationStatus.LOCKED, ReservationStatus.COMMITTED);
        ArgumentCaptor<InventoryOperation> operationCaptor =
                ArgumentCaptor.forClass(InventoryOperation.class);
        verify(operationMapper).insert(operationCaptor.capture());
        assertEquals("ORDER_COMMIT:RSV-PAID", operationCaptor.getValue().getBusinessKey());
        assertEquals(0, operationCaptor.getValue().getAvailableDelta());
        assertEquals(-3, operationCaptor.getValue().getLockedDelta());
        assertEquals(0, operationCaptor.getValue().getSoldDelta());
        verifyNoInteractions(productMapper, productSkuMapper);
    }

    @Test
    void confirmedCloseReleasesOnlyLockedReservationAndReplayIsNoOp() {
        OrderInfo order = order(11L, "ORDER-CLOSED", OrderStatus.CLOSED);
        OrderItem item = item(101L, 11L, 7L, 70L, 2, InventoryStatus.RESERVED);
        InventoryReservation locked = reservation(
                201L, "RSV-CLOSED", "ORDER-CLOSED", item, ReservationStatus.LOCKED);
        InventoryReservation released = reservation(
                201L, "RSV-CLOSED", "ORDER-CLOSED", item, ReservationStatus.RELEASED);
        when(orderInfoMapper.selectByOrderNoForUpdate("ORDER-CLOSED")).thenReturn(order);
        when(orderItemMapper.selectByOrderIdForUpdate(11L)).thenReturn(Collections.singletonList(item));
        when(reservationMapper.selectByOrderNoForUpdate("ORDER-CLOSED"))
                .thenReturn(Collections.singletonList(locked))
                .thenReturn(Collections.singletonList(released));
        when(skuStockMapper.selectBySkuIdForUpdate(70L)).thenReturn(stock(70L, 4, 2));
        when(skuStockMapper.releaseLockedStock(70L, 2)).thenReturn(1);
        when(reservationMapper.updateStatus(201L, ReservationStatus.LOCKED, ReservationStatus.RELEASED))
                .thenReturn(1);
        when(orderItemMapper.updateInventoryStatus(101L, InventoryStatus.RESERVED, InventoryStatus.RELEASED))
                .thenReturn(1);
        when(operationMapper.insert(any(InventoryOperation.class))).thenReturn(1);
        when(operationMapper.selectByBusinessKeyForUpdate("ORDER_RELEASE:RSV-CLOSED"))
                .thenReturn(null)
                .thenReturn(operation("ORDER_RELEASE:RSV-CLOSED", "ORDER-CLOSED", item, 2, -2));

        assertTrue(inventoryService.releaseReservation("ORDER-CLOSED"));
        assertFalse(inventoryService.releaseReservation("ORDER-CLOSED"));

        verify(skuStockMapper).releaseLockedStock(70L, 2);
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
            String orderNo,
            OrderItem item,
            ReservationStatus status
    ) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.setId(id);
        reservation.setReservationNo(reservationNo);
        reservation.setOrderNo(orderNo);
        reservation.setOrderItemId(item.getId());
        reservation.setSkuId(item.getSkuId());
        reservation.setQuantity(item.getQuantity());
        reservation.setStatus(status);
        return reservation;
    }

    private InventoryOperation operation(
            String businessKey,
            String orderNo,
            OrderItem item,
            int availableDelta,
            int lockedDelta
    ) {
        InventoryOperation operation = new InventoryOperation();
        operation.setBusinessKey(businessKey);
        operation.setOperationType(businessKey.substring(0, businessKey.indexOf(':')));
        operation.setOrderNo(orderNo);
        operation.setProductId(item.getProductId());
        operation.setSkuId(item.getSkuId());
        operation.setOrderItemId(item.getId());
        operation.setAvailableDelta(availableDelta);
        operation.setLockedDelta(lockedDelta);
        operation.setSoldDelta(0);
        return operation;
    }
}
