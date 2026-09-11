package cc.ivera.service.impl;

import cc.ivera.entity.InventoryOperation;
import cc.ivera.entity.InventoryReservation;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;
import cc.ivera.entity.Product;
import cc.ivera.entity.ProductSku;
import cc.ivera.entity.SkuStock;
import cc.ivera.enums.BusinessOrderStatus;
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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final String ORDER_RESERVE = "ORDER_RESERVE";
    private static final String ORDER_COMMIT = "ORDER_COMMIT";
    private static final String ORDER_RELEASE = "ORDER_RELEASE";
    private static final long FALLBACK_RESERVATION_MILLIS = 60_000L;

    private final OrderInfoMapper orderInfoMapper;
    private final OrderItemMapper orderItemMapper;
    private final ProductMapper productMapper;
    private final ProductSkuMapper productSkuMapper;
    private final SkuStockMapper skuStockMapper;
    private final InventoryReservationMapper reservationMapper;
    private final InventoryOperationMapper operationMapper;

    public InventoryServiceImpl(
            OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper,
            ProductMapper productMapper,
            ProductSkuMapper productSkuMapper,
            SkuStockMapper skuStockMapper,
            InventoryReservationMapper reservationMapper,
            InventoryOperationMapper operationMapper
    ) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.productSkuMapper = productSkuMapper;
        this.skuStockMapper = skuStockMapper;
        this.reservationMapper = reservationMapper;
        this.operationMapper = operationMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserve(OrderInfo order, List<OrderItem> items) {
        requireOrder(order);
        if (!isReservable(order)) {
            throw new ConflictException("订单状态不允许预占库存");
        }

        List<OrderItem> orderedItems = validateAndSortItems(items, true);
        int existingCount = 0;
        for (OrderItem item : orderedItems) {
            InventoryReservation existing = reservationMapper.selectByOrderItemIdForUpdate(item.getId());
            if (existing != null) {
                validateReservation(existing, order, item, ReservationStatus.LOCKED);
                validateOperation(
                        requireOperation(ORDER_RESERVE, existing.getReservationNo()),
                        ORDER_RESERVE,
                        order,
                        item,
                        -item.getQuantity(),
                        item.getQuantity()
                );
                existingCount++;
            }
        }
        if (existingCount == orderedItems.size()) {
            return;
        }
        if (existingCount > 0) {
            throw new ConflictException("订单库存预占记录不完整");
        }

        lockAndValidateProducts(orderedItems);
        Map<Long, Integer> quantities = aggregateBySku(orderedItems);
        Map<Long, SkuStock> stocks = lockAndPreflightStocks(quantities, true);

        Map<Long, InventoryReservation> reservations = new HashMap<>();
        for (OrderItem item : orderedItems) {
            InventoryReservation reservation = new InventoryReservation();
            reservation.setReservationNo(nextReservationNo());
            reservation.setOrderNo(order.getOrderNo());
            reservation.setOrderItemId(item.getId());
            reservation.setSkuId(item.getSkuId());
            reservation.setQuantity(item.getQuantity());
            reservation.setStatus(ReservationStatus.LOCKED);
            reservation.setExpiresAt(order.getExpiresAt() == null
                    ? new Date(System.currentTimeMillis() + FALLBACK_RESERVATION_MILLIS)
                    : order.getExpiresAt());
            if (reservationMapper.insert(reservation) != 1) {
                throw new ConflictException("库存预占记录写入失败");
            }
            reservations.put(item.getId(), reservation);
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            if (skuStockMapper.reserveStock(entry.getKey(), entry.getValue()) != 1) {
                throw new ConflictException("商品库存不足，请重试");
            }
        }
        writeReserveOperations(order, orderedItems, reservations, stocks);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean preflightAndCommit(String orderNo) {
        OrderInfo order = lockOrder(orderNo);
        if (!isCommitEligible(order)) {
            return false;
        }
        return transitionReservations(order, ReservationStatus.COMMITTED, ORDER_COMMIT);
    }

    @Override
    public boolean commitPayment(String orderNo) {
        return preflightAndCommit(orderNo);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseReservation(String orderNo) {
        OrderInfo order = lockOrder(orderNo);
        if (!isReleaseEligible(order)) {
            return false;
        }
        return transitionReservations(order, ReservationStatus.RELEASED, ORDER_RELEASE);
    }

    private boolean transitionReservations(
            OrderInfo order,
            ReservationStatus targetStatus,
            String operationType
    ) {
        List<OrderItem> items = orderItemMapper.selectByOrderIdForUpdate(order.getId());
        if (items == null || items.isEmpty()) {
            throw new ConflictException("订单明细不存在，无法变更库存");
        }
        Map<Long, OrderItem> itemById = new HashMap<>();
        for (OrderItem item : validateAndSortItems(items, false)) {
            itemById.put(item.getId(), item);
        }

        List<InventoryReservation> reservations = reservationMapper.selectByOrderNoForUpdate(order.getOrderNo());
        if (reservations == null || reservations.size() != itemById.size()) {
            throw new ConflictException("订单库存预占记录不完整");
        }
        List<InventoryReservation> orderedReservations = new ArrayList<>(reservations);
        orderedReservations.sort(Comparator.comparing(InventoryReservation::getSkuId)
                .thenComparing(InventoryReservation::getId));

        boolean allTarget = true;
        for (InventoryReservation reservation : orderedReservations) {
            OrderItem item = itemById.get(reservation.getOrderItemId());
            if (item == null) {
                throw new ConflictException("库存预占关联订单明细不存在");
            }
            validateReservationAssociation(reservation, order, item);
            if (reservation.getStatus() != targetStatus) {
                allTarget = false;
            }
        }

        if (allTarget) {
            for (InventoryReservation reservation : orderedReservations) {
                OrderItem item = itemById.get(reservation.getOrderItemId());
                int availableDelta = targetStatus == ReservationStatus.RELEASED
                        ? reservation.getQuantity() : 0;
                validateOperation(
                        requireOperation(operationType, reservation.getReservationNo()),
                        operationType,
                        order,
                        item,
                        availableDelta,
                        -reservation.getQuantity()
                );
            }
            return false;
        }

        for (InventoryReservation reservation : orderedReservations) {
            if (reservation.getStatus() != ReservationStatus.LOCKED) {
                throw new ConflictException("订单库存预占状态不一致");
            }
            String key = businessKey(operationType, reservation.getReservationNo());
            if (operationMapper.selectByBusinessKeyForUpdate(key) != null) {
                throw new ConflictException("库存流水与预占状态不一致");
            }
        }

        Map<Long, Integer> quantities = aggregateReservationsBySku(orderedReservations);
        Map<Long, SkuStock> stocks = lockAndPreflightStocks(quantities, false);
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            int updated = targetStatus == ReservationStatus.COMMITTED
                    ? skuStockMapper.commitLockedStock(entry.getKey(), entry.getValue())
                    : skuStockMapper.releaseLockedStock(entry.getKey(), entry.getValue());
            if (updated != 1) {
                throw new ConflictException("锁定库存不足，订单库存状态异常");
            }
        }

        writeTransitionOperations(order, orderedReservations, itemById, stocks, targetStatus, operationType);
        InventoryStatus projectionTarget = targetStatus == ReservationStatus.COMMITTED
                ? InventoryStatus.SOLD : InventoryStatus.RELEASED;
        for (InventoryReservation reservation : orderedReservations) {
            if (reservation.getId() == null
                    || reservationMapper.updateStatus(
                            reservation.getId(),
                            ReservationStatus.LOCKED,
                            targetStatus
                    ) != 1) {
                throw new ConflictException("库存预占状态已变化，请重试");
            }
            OrderItem item = itemById.get(reservation.getOrderItemId());
            if (item.getInventoryStatus() == InventoryStatus.RESERVED) {
                if (orderItemMapper.updateInventoryStatus(
                        item.getId(), InventoryStatus.RESERVED, projectionTarget) != 1) {
                    throw new ConflictException("订单明细库存投影已变化，请重试");
                }
            } else if (item.getInventoryStatus() != projectionTarget) {
                throw new ConflictException("订单明细库存投影不一致");
            }
        }
        return true;
    }

    private void writeReserveOperations(
            OrderInfo order,
            List<OrderItem> items,
            Map<Long, InventoryReservation> reservations,
            Map<Long, SkuStock> stocks
    ) {
        Map<Long, Integer> available = new HashMap<>();
        Map<Long, Integer> locked = new HashMap<>();
        for (Map.Entry<Long, SkuStock> entry : stocks.entrySet()) {
            available.put(entry.getKey(), requiredStock(entry.getValue().getAvailableStock()));
            locked.put(entry.getKey(), requiredStock(entry.getValue().getLockedStock()));
        }
        for (OrderItem item : itemsBySku(items)) {
            Long skuId = item.getSkuId();
            int quantity = item.getQuantity();
            int availableBefore = available.get(skuId);
            int lockedBefore = locked.get(skuId);
            InventoryReservation reservation = reservations.get(item.getId());
            insertOperation(operation(
                    businessKey(ORDER_RESERVE, reservation.getReservationNo()),
                    ORDER_RESERVE,
                    order,
                    item,
                    -quantity,
                    quantity,
                    availableBefore,
                    lockedBefore,
                    "订单创建预占库存"
            ));
            available.put(skuId, Math.subtractExact(availableBefore, quantity));
            locked.put(skuId, Math.addExact(lockedBefore, quantity));
        }
    }

    private void writeTransitionOperations(
            OrderInfo order,
            List<InventoryReservation> reservations,
            Map<Long, OrderItem> itemById,
            Map<Long, SkuStock> stocks,
            ReservationStatus targetStatus,
            String operationType
    ) {
        Map<Long, Integer> available = new HashMap<>();
        Map<Long, Integer> locked = new HashMap<>();
        for (Map.Entry<Long, SkuStock> entry : stocks.entrySet()) {
            available.put(entry.getKey(), requiredStock(entry.getValue().getAvailableStock()));
            locked.put(entry.getKey(), requiredStock(entry.getValue().getLockedStock()));
        }
        for (InventoryReservation reservation : reservations) {
            OrderItem item = itemById.get(reservation.getOrderItemId());
            Long skuId = reservation.getSkuId();
            int quantity = reservation.getQuantity();
            int availableBefore = available.get(skuId);
            int lockedBefore = locked.get(skuId);
            int availableDelta = targetStatus == ReservationStatus.RELEASED ? quantity : 0;
            insertOperation(operation(
                    businessKey(operationType, reservation.getReservationNo()),
                    operationType,
                    order,
                    item,
                    availableDelta,
                    -quantity,
                    availableBefore,
                    lockedBefore,
                    targetStatus == ReservationStatus.COMMITTED
                            ? "订单支付成功提交预占库存"
                            : "订单关闭释放预占库存"
            ));
            available.put(skuId, Math.addExact(availableBefore, availableDelta));
            locked.put(skuId, Math.subtractExact(lockedBefore, quantity));
        }
    }

    private List<OrderItem> validateAndSortItems(List<OrderItem> items, boolean reserving) {
        if (items == null || items.isEmpty()) {
            throw new ConflictException("订单明细不能为空");
        }
        List<OrderItem> ordered = new ArrayList<>(items);
        for (OrderItem item : ordered) {
            if (item == null
                    || item.getId() == null
                    || item.getProductId() == null
                    || item.getSkuId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() < 1
                    || (reserving && item.getInventoryStatus() != InventoryStatus.RESERVED)) {
                throw new ConflictException("订单明细库存信息无效");
            }
        }
        ordered.sort(Comparator.comparing(OrderItem::getProductId).thenComparing(OrderItem::getId));
        return ordered;
    }

    private void lockAndValidateProducts(List<OrderItem> items) {
        List<Long> productIds = new ArrayList<>();
        for (OrderItem item : items) {
            if (!productIds.contains(item.getProductId())) {
                productIds.add(item.getProductId());
            }
        }
        Collections.sort(productIds);
        for (Long productId : productIds) {
            Product product = productMapper.selectByIdForUpdate(productId);
            if (product == null) {
                throw new ConflictException("商品不存在");
            }
            if (product.getStatus() != ProductStatus.ON_SHELF) {
                throw new ConflictException("商品已下架，无法预占库存");
            }
        }
        for (OrderItem item : items) {
            ProductSku sku = productSkuMapper.selectById(item.getSkuId());
            if (sku == null
                    || !Objects.equals(item.getProductId(), sku.getProductId())
                    || !"ACTIVE".equals(sku.getStatus())) {
                throw new ConflictException("商品SKU不存在或不可用");
            }
        }
    }

    private Map<Long, Integer> aggregateBySku(List<OrderItem> items) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        List<OrderItem> ordered = itemsBySku(items);
        try {
            for (OrderItem item : ordered) {
                quantities.merge(item.getSkuId(), item.getQuantity(), Math::addExact);
            }
        } catch (ArithmeticException exception) {
            throw new ConflictException("订单商品数量超出范围");
        }
        return quantities;
    }

    private Map<Long, Integer> aggregateReservationsBySku(List<InventoryReservation> reservations) {
        Map<Long, Integer> quantities = new LinkedHashMap<>();
        try {
            for (InventoryReservation reservation : reservations) {
                quantities.merge(reservation.getSkuId(), reservation.getQuantity(), Math::addExact);
            }
        } catch (ArithmeticException exception) {
            throw new ConflictException("订单预占数量超出范围");
        }
        return quantities;
    }

    private Map<Long, SkuStock> lockAndPreflightStocks(
            Map<Long, Integer> quantities,
            boolean checkAvailable
    ) {
        Map<Long, SkuStock> stocks = new LinkedHashMap<>();
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            SkuStock stock = skuStockMapper.selectBySkuIdForUpdate(entry.getKey());
            if (stock == null) {
                throw new ConflictException("SKU库存记录不存在");
            }
            int available = requiredStock(stock.getAvailableStock());
            int locked = requiredStock(stock.getLockedStock());
            if (checkAvailable && available < entry.getValue()) {
                throw new ConflictException("商品库存不足");
            }
            if (!checkAvailable && locked < entry.getValue()) {
                throw new ConflictException("锁定库存不足，订单库存状态异常");
            }
            stocks.put(entry.getKey(), stock);
        }
        return stocks;
    }

    private List<OrderItem> itemsBySku(List<OrderItem> items) {
        List<OrderItem> ordered = new ArrayList<>(items);
        ordered.sort(Comparator.comparing(OrderItem::getSkuId).thenComparing(OrderItem::getId));
        return ordered;
    }

    private void validateReservation(
            InventoryReservation reservation,
            OrderInfo order,
            OrderItem item,
            ReservationStatus expectedStatus
    ) {
        validateReservationAssociation(reservation, order, item);
        if (reservation.getStatus() != expectedStatus) {
            throw new ConflictException("库存预占幂等请求状态冲突");
        }
    }

    private void validateReservationAssociation(
            InventoryReservation reservation,
            OrderInfo order,
            OrderItem item
    ) {
        if (!Objects.equals(order.getOrderNo(), reservation.getOrderNo())
                || !Objects.equals(item.getId(), reservation.getOrderItemId())
                || !Objects.equals(item.getSkuId(), reservation.getSkuId())
                || !Objects.equals(item.getQuantity(), reservation.getQuantity())) {
            throw new ConflictException("库存预占业务键参数冲突");
        }
    }

    private InventoryOperation requireOperation(String operationType, String reservationNo) {
        InventoryOperation operation = operationMapper.selectByBusinessKeyForUpdate(
                businessKey(operationType, reservationNo));
        if (operation == null) {
            throw new ConflictException("库存预占与流水不完整");
        }
        return operation;
    }

    private void validateOperation(
            InventoryOperation operation,
            String operationType,
            OrderInfo order,
            OrderItem item,
            int availableDelta,
            int lockedDelta
    ) {
        if (!operationType.equals(operation.getOperationType())
                || !Objects.equals(order.getOrderNo(), operation.getOrderNo())
                || !Objects.equals(item.getProductId(), operation.getProductId())
                || !Objects.equals(item.getSkuId(), operation.getSkuId())
                || !Objects.equals(item.getId(), operation.getOrderItemId())
                || !Objects.equals(availableDelta, operation.getAvailableDelta())
                || !Objects.equals(lockedDelta, operation.getLockedDelta())
                || !Objects.equals(0, operation.getSoldDelta())) {
            throw new ConflictException("库存流水业务键参数冲突");
        }
    }

    private InventoryOperation operation(
            String businessKey,
            String operationType,
            OrderInfo order,
            OrderItem item,
            int availableDelta,
            int lockedDelta,
            int availableBefore,
            int lockedBefore,
            String reason
    ) {
        InventoryOperation operation = new InventoryOperation();
        operation.setBusinessKey(businessKey);
        operation.setProductId(item.getProductId());
        operation.setSkuId(item.getSkuId());
        operation.setOrderItemId(item.getId());
        operation.setOperationType(operationType);
        operation.setOrderNo(order.getOrderNo());
        operation.setAvailableDelta(availableDelta);
        operation.setLockedDelta(lockedDelta);
        operation.setSoldDelta(0);
        operation.setAvailableBefore(availableBefore);
        operation.setAvailableAfter(Math.addExact(availableBefore, availableDelta));
        operation.setLockedBefore(lockedBefore);
        operation.setLockedAfter(Math.addExact(lockedBefore, lockedDelta));
        operation.setSoldBefore(0);
        operation.setSoldAfter(0);
        operation.setReason(reason);
        return operation;
    }

    private void insertOperation(InventoryOperation operation) {
        if (operationMapper.insert(operation) != 1) {
            throw new ConflictException("库存流水写入失败");
        }
    }

    private OrderInfo lockOrder(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            throw new ConflictException("订单号不能为空");
        }
        OrderInfo order = orderInfoMapper.selectByOrderNoForUpdate(orderNo);
        requireOrder(order);
        return order;
    }

    private void requireOrder(OrderInfo order) {
        if (order == null || order.getId() == null || !StringUtils.hasText(order.getOrderNo())) {
            throw new ConflictException("订单不存在");
        }
    }

    private boolean isReservable(OrderInfo order) {
        return OrderStatus.NOTPAY.getType().equals(order.getOrderStatus())
                || BusinessOrderStatus.WAIT_PAY.name().equals(order.getOrderStatus());
    }

    private boolean isCommitEligible(OrderInfo order) {
        return OrderStatus.SUCCESS.getType().equals(order.getOrderStatus())
                || (BusinessOrderStatus.ACTIVE.name().equals(order.getOrderStatus())
                && "PAID".equals(order.getPayStatus()));
    }

    private boolean isReleaseEligible(OrderInfo order) {
        return OrderStatus.CLOSED.getType().equals(order.getOrderStatus())
                || OrderStatus.CANCEL.getType().equals(order.getOrderStatus())
                || BusinessOrderStatus.CLOSED.name().equals(order.getOrderStatus());
    }

    private int requiredStock(Integer value) {
        if (value == null || value < 0) {
            throw new ConflictException("SKU库存余额非法");
        }
        return value;
    }

    private String nextReservationNo() {
        return "RSV" + UUID.randomUUID().toString().replace("-", "");
    }

    private String businessKey(String operationType, String reservationNo) {
        return operationType + ":" + reservationNo;
    }
}
