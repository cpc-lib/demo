package cc.ivera.service.impl;

import cc.ivera.entity.OrderIdempotency;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;
import cc.ivera.entity.Product;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.InventoryStatus;
import cc.ivera.enums.ProductStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.OrderCloseMessageService;
import cc.ivera.service.InventoryService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.OrderIdempotencyService;
import cc.ivera.security.AuthUser;
import cc.ivera.security.AuthContext;
import cc.ivera.util.OrderNoUtils;
import cc.ivera.util.OrderRequestFingerprint;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Objects;

@Service
@Slf4j
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo> implements OrderInfoService {

    private static final long ORDER_CREATE_LOCK_WAIT_MS = 3000L;
    private static final long ORDER_CREATE_LOCK_LEASE_MS = -1L;

    private final ProductMapper productMapper;

    private final OrderCloseMessageService orderCloseMessageService;

    private final DistributedLockTemplate distributedLockTemplate;

    private final TransactionTemplate transactionTemplate;

    private final OrderItemMapper orderItemMapper;

    private final OrderIdempotencyService orderIdempotencyService;

    private final InventoryService inventoryService;

    public OrderInfoServiceImpl(
        ProductMapper productMapper,
        OrderCloseMessageService orderCloseMessageService,
        DistributedLockTemplate distributedLockTemplate,
        TransactionTemplate transactionTemplate,
        OrderItemMapper orderItemMapper,
        OrderIdempotencyService orderIdempotencyService,
        InventoryService inventoryService
    ) {
        this.productMapper = productMapper;
        this.orderCloseMessageService = orderCloseMessageService;
        this.distributedLockTemplate = distributedLockTemplate;
        this.transactionTemplate = transactionTemplate;
        this.orderItemMapper = orderItemMapper;
        this.orderIdempotencyService = orderIdempotencyService;
        this.inventoryService = inventoryService;
    }

    @Override
    public OrderInfo createOrReuseOrder(
            Long productId,
            String paymentType,
            Long paymentAppId,
            String paymentChannelCode,
            String idempotencyKey
    ) {
        AuthUser authUser = AuthContext.requireShoppingUser();
        validateCreateOrderParams(productId, paymentType);
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > 64) {
            throw new ConflictException("订单幂等键无效");
        }
        Long userId = authUser.getUserId();
        String requestFingerprint = OrderRequestFingerprint.directBuy(
                productId,
                1,
                paymentAppId,
                paymentChannelCode
        );
        String lockKey = "payment:order:create:" + userId + ":" + idempotencyKey;
        return distributedLockTemplate.execute(
                lockKey,
                ORDER_CREATE_LOCK_WAIT_MS,
                ORDER_CREATE_LOCK_LEASE_MS,
                () -> {
                    orderIdempotencyService.expireIssuedKeyIfNeeded(userId, idempotencyKey);
                    return transactionTemplate.execute(status -> doCreateOrReuseOrder(
                            productId,
                            paymentType,
                            paymentAppId,
                            paymentChannelCode,
                            userId,
                            idempotencyKey,
                            requestFingerprint
                    ));
                }
        );
    }

    private OrderInfo doCreateOrReuseOrder(
            Long productId,
            String paymentType,
            Long paymentAppId,
            String paymentChannelCode,
            Long userId,
            String idempotencyKey,
            String requestFingerprint
    ) {
        OrderIdempotency idempotency = orderIdempotencyService.requireForUpdate(
                userId,
                idempotencyKey,
                requestFingerprint
        );
        if ("COMPLETED".equals(idempotency.getStatus())) {
            OrderInfo existing = baseMapper.selectById(idempotency.getOrderId());
            if (existing == null || !Objects.equals(userId, existing.getUserId())) {
                throw new ConflictException("订单幂等键关联订单不存在");
            }
            return existing;
        }

        Product product = productMapper.selectByIdForUpdate(productId);
        if (product == null) {
            throw new ConflictException("商品不存在或已删除");
        }
        if (product.getStatus() != ProductStatus.ON_SHELF) {
            throw new ConflictException("商品已下架");
        }
        if (product.getAvailableStock() == null || product.getAvailableStock() < 1) {
            throw new ConflictException("商品库存不足");
        }
        if (product.getPrice() == null || product.getPrice() < 0) {
            throw new BizException("商品价格无效");
        }

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setTitle(product.getTitle());
        orderInfo.setOrderNo(OrderNoUtils.getOrderNo());
        orderInfo.setProductId(productId);
        orderInfo.setUserId(userId);
        orderInfo.setTotalFee(product.getPrice());
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        orderInfo.setPaymentType(paymentType);
        orderInfo.setPaymentAppId(paymentAppId);
        orderInfo.setPaymentChannelCode(paymentChannelCode);
        orderInfo.setCheckoutRequestId(idempotencyKey);
        orderInfo.setVersion(0);

        if (baseMapper.insert(orderInfo) != 1 || orderInfo.getId() == null) {
            throw new BizException("订单创建失败");
        }

        OrderItem orderItem = new OrderItem();
        orderItem.setOrderId(orderInfo.getId());
        orderItem.setProductId(product.getId());
        orderItem.setProductTitle(product.getTitle());
        orderItem.setUnitPrice(product.getPrice());
        orderItem.setQuantity(1);
        orderItem.setSubtotal(product.getPrice());
        orderItem.setInventoryStatus(InventoryStatus.RESERVED);
        orderItem.setRefundedQuantity(0);
        if (orderItemMapper.insert(orderItem) != 1) {
            throw new BizException("订单明细创建失败");
        }

        inventoryService.reserve(orderInfo, java.util.Collections.singletonList(orderItem));
        orderCloseMessageService.sendCloseOrderMessage(orderInfo.getOrderNo(), orderInfo.getPaymentType());
        orderIdempotencyService.complete(idempotency.getId(), requestFingerprint, orderInfo.getId());
        return orderInfo;
    }

    @Override
    public void saveCodeUrl(String orderNo, String codeUrl) {
        if (!StringUtils.hasText(orderNo) || !StringUtils.hasText(codeUrl)) {
            return;
        }

        UpdateWrapper<OrderInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo)
                .and(wrapper -> wrapper.isNull("code_url").or().eq("code_url", ""));

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setCodeUrl(codeUrl);

        int updated = baseMapper.update(orderInfo, updateWrapper);
        if (updated == 0) {
            log.info("订单二维码已存在，本次不覆盖，orderNo={}", orderNo);
        }
    }

    @Override
    public List<OrderInfo> listOrderByCreateTimeDesc() {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<OrderInfo>().orderByDesc("create_time");
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<OrderInfo> listOrderByUserId(Long userId) {
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<OrderInfo>()
                .eq("user_id", userId)
                .orderByDesc("create_time");
        return baseMapper.selectList(queryWrapper);
    }

    @Override
    public List<OrderItem> listOrderItemsForUser(String orderNo, AuthUser authUser) {
        OrderInfo order = requireAccessibleOrder(orderNo, authUser);
        return orderItemMapper.selectByOrderId(order.getId());
    }

    @Override
    public void updateStatusByOrderNo(String orderNo, OrderStatus orderStatus) {
        if (!StringUtils.hasText(orderNo) || orderStatus == null) {
            return;
        }
        log.info("更新订单状态 ===> orderNo={}, status={}", orderNo, orderStatus.getType());

        UpdateWrapper<OrderInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(orderStatus.getType());

        baseMapper.update(orderInfo, updateWrapper);
    }

    @Override
    public boolean updateStatusByOrderNoIfStatus(String orderNo, OrderStatus currentStatus, OrderStatus targetStatus) {
        if (!StringUtils.hasText(orderNo) || currentStatus == null || targetStatus == null) {
            return false;
        }

        log.info("条件更新订单状态 ===> orderNo={}, {} -> {}",
                orderNo,
                currentStatus.getType(),
                targetStatus.getType());

        UpdateWrapper<OrderInfo> updateWrapper = new UpdateWrapper<>();
        updateWrapper.eq("order_no", orderNo);
        updateWrapper.eq("order_status", currentStatus.getType());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderStatus(targetStatus.getType());

        return baseMapper.update(orderInfo, updateWrapper) > 0;
    }

    @Override
    public String getOrderStatus(String orderNo) {
        OrderInfo orderInfo = getOrderByOrderNo(orderNo);
        return orderInfo == null ? null : orderInfo.getOrderStatus();
    }

    @Override
    public String getOrderStatusForUser(String orderNo, AuthUser authUser) {
        return requireAccessibleOrder(orderNo, authUser).getOrderStatus();
    }

    @Override
    public OrderInfo getOrderByOrderNo(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        QueryWrapper<OrderInfo> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("order_no", orderNo).last("limit 1");
        return baseMapper.selectOne(queryWrapper);
    }

    @Override
    public OrderInfo getOrderByOrderNoForUpdate(String orderNo) {
        if (!StringUtils.hasText(orderNo)) {
            return null;
        }
        return baseMapper.selectByOrderNoForUpdate(orderNo);
    }

    @Override
    public OrderInfo getOrderForUser(String orderNo, AuthUser authUser) {
        return requireAccessibleOrder(orderNo, authUser);
    }

    private OrderInfo requireAccessibleOrder(String orderNo, AuthUser authUser) {
        OrderInfo order = getOrderByOrderNo(orderNo);
        if (order == null) {
            throw new BizException("订单不存在");
        }
        if (authUser.getRole() != UserRole.ADMIN && !authUser.getUserId().equals(order.getUserId())) {
            throw new ForbiddenException("无权访问该订单");
        }
        return order;
    }

    private void validateCreateOrderParams(Long productId, String paymentType) {
        if (productId == null) {
            throw new BizException("商品ID不能为空");
        }
        if (!StringUtils.hasText(paymentType)) {
            throw new BizException("支付方式不能为空");
        }
    }

}
