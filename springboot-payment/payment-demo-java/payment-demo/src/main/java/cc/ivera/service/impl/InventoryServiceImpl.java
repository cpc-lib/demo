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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class InventoryServiceImpl implements InventoryService {

    private static final String ORDER_RESERVE = "ORDER_RESERVE";
    private static final String ORDER_COMMIT = "ORDER_COMMIT";
    private static final String ORDER_RELEASE = "ORDER_RELEASE";

    private final OrderInfoMapper orderInfoMapper;

    private final OrderItemMapper orderItemMapper;

    private final ProductMapper productMapper;

    private final InventoryOperationMapper operationMapper;

    public InventoryServiceImpl(
            OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper,
            ProductMapper productMapper,
            InventoryOperationMapper operationMapper
    ) {
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.productMapper = productMapper;
        this.operationMapper = operationMapper;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void reserve(OrderInfo order, List<OrderItem> items) {
        requireOrder(order);
        if (!OrderStatus.NOTPAY.getType().equals(order.getOrderStatus())) {
            throw new ConflictException("订单状态不允许预占库存");
        }

        Map<Long, Integer> quantities = aggregateQuantities(items, InventoryStatus.RESERVED, true);
        Map<Long, Product> products = lockProducts(quantities);
        int existingOperations = 0;
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            String businessKey = businessKey(ORDER_RESERVE, order.getOrderNo(), productId);
            InventoryOperation existing = operationMapper.selectByBusinessKeyForUpdate(businessKey);
            if (existing != null) {
                validateExistingOperation(existing, order.getOrderNo(), productId, ORDER_RESERVE,
                        -quantity, quantity, 0);
                existingOperations++;
            }
        }
        if (existingOperations == quantities.size()) {
            return;
        }
        if (existingOperations > 0) {
            throw new ConflictException("订单预占库存流水不完整");
        }

        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            Product product = products.get(productId);
            String businessKey = businessKey(ORDER_RESERVE, order.getOrderNo(), productId);
            if (product.getStatus() != ProductStatus.ON_SHELF) {
                throw new ConflictException("商品已下架，无法预占库存");
            }
            if (stock(product.getAvailableStock()) < quantity) {
                throw new ConflictException("商品库存不足");
            }
            if (productMapper.reserveStock(productId, quantity) != 1) {
                throw new ConflictException("商品库存不足，请重试");
            }
            insertOperation(operation(
                    businessKey,
                    ORDER_RESERVE,
                    order.getOrderNo(),
                    product,
                    -quantity,
                    quantity,
                    0,
                    "订单创建预占库存"
            ));
        }
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean commitPayment(String orderNo) {
        OrderInfo order = lockOrder(orderNo);
        if (!OrderStatus.SUCCESS.getType().equals(order.getOrderStatus())) {
            return false;
        }
        return transitionItems(order, InventoryStatus.SOLD, ORDER_COMMIT);
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public boolean releaseReservation(String orderNo) {
        OrderInfo order = lockOrder(orderNo);
        if (!OrderStatus.CLOSED.getType().equals(order.getOrderStatus())
                && !OrderStatus.CANCEL.getType().equals(order.getOrderStatus())) {
            return false;
        }
        return transitionItems(order, InventoryStatus.RELEASED, ORDER_RELEASE);
    }

    private boolean transitionItems(
            OrderInfo order,
            InventoryStatus targetStatus,
            String operationType
    ) {
        List<OrderItem> allItems = orderItemMapper.selectByOrderIdForUpdate(order.getId());
        if (allItems == null || allItems.isEmpty()) {
            throw new ConflictException("订单明细不存在，无法变更库存");
        }
        if (allItems.stream().allMatch(item -> item.getInventoryStatus() == targetStatus)) {
            return false;
        }
        if (allItems.stream().anyMatch(item -> item.getInventoryStatus() != InventoryStatus.RESERVED)) {
            throw new ConflictException("订单明细库存状态不一致");
        }
        List<OrderItem> reservedItems = allItems.stream()
                .sorted(Comparator.comparing(OrderItem::getProductId).thenComparing(OrderItem::getId))
                .collect(Collectors.toList());

        Map<Long, Integer> quantities = aggregateQuantities(reservedItems, InventoryStatus.RESERVED, false);
        Map<Long, Product> products = lockProducts(quantities);
        for (Map.Entry<Long, Integer> entry : quantities.entrySet()) {
            Long productId = entry.getKey();
            int quantity = entry.getValue();
            Product product = products.get(productId);
            String businessKey = businessKey(operationType, order.getOrderNo(), productId);
            if (operationMapper.selectByBusinessKeyForUpdate(businessKey) != null) {
                throw new ConflictException("库存流水与订单明细状态不一致");
            }

            int updated;
            int availableDelta;
            int lockedDelta = -quantity;
            int soldDelta;
            String reason;
            if (targetStatus == InventoryStatus.SOLD) {
                updated = productMapper.commitReservedStock(productId, quantity);
                availableDelta = 0;
                soldDelta = quantity;
                reason = "订单支付成功扣减锁定库存";
            } else {
                updated = productMapper.releaseReservedStock(productId, quantity);
                availableDelta = quantity;
                soldDelta = 0;
                reason = "订单关闭释放锁定库存";
            }
            if (updated != 1) {
                throw new ConflictException("锁定库存不足，订单库存状态异常");
            }
            insertOperation(operation(
                    businessKey,
                    operationType,
                    order.getOrderNo(),
                    product,
                    availableDelta,
                    lockedDelta,
                    soldDelta,
                    reason
            ));
        }

        for (OrderItem item : reservedItems) {
            if (item.getId() == null
                    || orderItemMapper.updateInventoryStatus(
                            item.getId(),
                            InventoryStatus.RESERVED,
                            targetStatus
                    ) != 1) {
                throw new ConflictException("订单明细库存状态已变化，请重试");
            }
        }
        return true;
    }

    private Map<Long, Integer> aggregateQuantities(
            List<OrderItem> items,
            InventoryStatus expectedStatus,
            boolean allowMissingId
    ) {
        if (items == null || items.isEmpty()) {
            throw new ConflictException("订单明细不能为空");
        }
        Map<Long, Integer> unsorted = new LinkedHashMap<>();
        for (OrderItem item : items) {
            if (item == null
                    || (!allowMissingId && item.getId() == null)
                    || item.getProductId() == null
                    || item.getQuantity() == null
                    || item.getQuantity() < 1
                    || item.getInventoryStatus() != expectedStatus) {
                throw new ConflictException("订单明细库存信息无效");
            }
            unsorted.merge(item.getProductId(), item.getQuantity(), Math::addExact);
        }
        List<Long> productIds = new ArrayList<>(unsorted.keySet());
        Collections.sort(productIds);
        Map<Long, Integer> sorted = new LinkedHashMap<>();
        for (Long productId : productIds) {
            sorted.put(productId, unsorted.get(productId));
        }
        return sorted;
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

    private Product requireLockedProduct(Long productId) {
        Product product = productMapper.selectByIdForUpdate(productId);
        if (product == null) {
            throw new ConflictException("商品不存在");
        }
        return product;
    }

    private Map<Long, Product> lockProducts(Map<Long, Integer> quantities) {
        Map<Long, Product> products = new LinkedHashMap<>();
        for (Long productId : quantities.keySet()) {
            products.put(productId, requireLockedProduct(productId));
        }
        return products;
    }

    private void validateExistingOperation(
            InventoryOperation operation,
            String orderNo,
            Long productId,
            String operationType,
            int availableDelta,
            int lockedDelta,
            int soldDelta
    ) {
        if (!operationType.equals(operation.getOperationType())
                || !orderNo.equals(operation.getOrderNo())
                || !productId.equals(operation.getProductId())
                || !Integer.valueOf(availableDelta).equals(operation.getAvailableDelta())
                || !Integer.valueOf(lockedDelta).equals(operation.getLockedDelta())
                || !Integer.valueOf(soldDelta).equals(operation.getSoldDelta())) {
            throw new ConflictException("库存流水业务键参数冲突");
        }
    }

    private InventoryOperation operation(
            String businessKey,
            String operationType,
            String orderNo,
            Product product,
            int availableDelta,
            int lockedDelta,
            int soldDelta,
            String reason
    ) {
        int availableBefore = stock(product.getAvailableStock());
        int lockedBefore = stock(product.getLockedStock());
        int soldBefore = stock(product.getSoldStock());
        InventoryOperation operation = new InventoryOperation();
        operation.setBusinessKey(businessKey);
        operation.setProductId(product.getId());
        operation.setOperationType(operationType);
        operation.setOrderNo(orderNo);
        operation.setAvailableDelta(availableDelta);
        operation.setLockedDelta(lockedDelta);
        operation.setSoldDelta(soldDelta);
        operation.setAvailableBefore(availableBefore);
        operation.setAvailableAfter(Math.addExact(availableBefore, availableDelta));
        operation.setLockedBefore(lockedBefore);
        operation.setLockedAfter(Math.addExact(lockedBefore, lockedDelta));
        operation.setSoldBefore(soldBefore);
        operation.setSoldAfter(Math.addExact(soldBefore, soldDelta));
        operation.setReason(reason);
        return operation;
    }

    private void insertOperation(InventoryOperation operation) {
        if (operationMapper.insert(operation) != 1) {
            throw new ConflictException("库存流水写入失败");
        }
    }

    private String businessKey(String operationType, String orderNo, Long productId) {
        return operationType + ":" + orderNo + ":" + productId;
    }

    private int stock(Integer value) {
        return value == null ? 0 : value;
    }
}
