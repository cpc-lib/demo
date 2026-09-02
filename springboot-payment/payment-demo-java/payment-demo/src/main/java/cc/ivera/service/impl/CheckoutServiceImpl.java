package cc.ivera.service.impl;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.Cart;
import cc.ivera.entity.CartItem;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderIdempotency;
import cc.ivera.entity.OrderItem;
import cc.ivera.entity.Product;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.InventoryStatus;
import cc.ivera.enums.PayType;
import cc.ivera.enums.ProductStatus;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.CartItemMapper;
import cc.ivera.mapper.CartMapper;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.CheckoutService;
import cc.ivera.service.InventoryService;
import cc.ivera.service.OrderCloseMessageService;
import cc.ivera.service.OrderIdempotencyService;
import cc.ivera.util.OrderNoUtils;
import cc.ivera.vo.CheckoutResult;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private static final long CHECKOUT_LOCK_WAIT_MS = 3000L;

    private static final long CHECKOUT_LOCK_LEASE_MS = -1L;

    private final CartMapper cartMapper;

    private final CartItemMapper cartItemMapper;

    private final ProductMapper productMapper;

    private final OrderInfoMapper orderInfoMapper;

    private final OrderItemMapper orderItemMapper;

    private final PaymentConfigLoader paymentConfigLoader;

    private final OrderCloseMessageService closeMessageService;

    private final OrderIdempotencyService orderIdempotencyService;

    private final InventoryService inventoryService;

    private final DistributedLockTemplate lockTemplate;

    private final TransactionTemplate transactionTemplate;

    public CheckoutServiceImpl(
            CartMapper cartMapper,
            CartItemMapper cartItemMapper,
            ProductMapper productMapper,
            OrderInfoMapper orderInfoMapper,
            OrderItemMapper orderItemMapper,
            PaymentConfigLoader paymentConfigLoader,
            OrderCloseMessageService closeMessageService,
            OrderIdempotencyService orderIdempotencyService,
            InventoryService inventoryService,
            DistributedLockTemplate lockTemplate,
            TransactionTemplate transactionTemplate
    ) {
        this.cartMapper = cartMapper;
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
        this.orderInfoMapper = orderInfoMapper;
        this.orderItemMapper = orderItemMapper;
        this.paymentConfigLoader = paymentConfigLoader;
        this.closeMessageService = closeMessageService;
        this.orderIdempotencyService = orderIdempotencyService;
        this.inventoryService = inventoryService;
        this.lockTemplate = lockTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public CheckoutResult checkout(
            Long userId,
            Long paymentAppId,
            String idempotencyKey,
            String requestFingerprint
    ) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }
        if (paymentAppId == null) {
            throw new BizException("支付应用不能为空");
        }
        if (!StringUtils.hasText(idempotencyKey) || idempotencyKey.length() > 64) {
            throw new ConflictException("订单幂等键无效");
        }
        if (!StringUtils.hasText(requestFingerprint) || requestFingerprint.length() > 64) {
            throw new ConflictException("订单请求指纹无效");
        }

        return lockTemplate.execute(
                "payment:order:create:" + userId + ":" + idempotencyKey,
                CHECKOUT_LOCK_WAIT_MS,
                CHECKOUT_LOCK_LEASE_MS,
                () -> {
                    orderIdempotencyService.expireIssuedKeyIfNeeded(userId, idempotencyKey);
                    return transactionTemplate.execute(status -> doCheckout(
                            userId,
                            paymentAppId,
                            idempotencyKey,
                            requestFingerprint
                    ));
                }
        );
    }

    private CheckoutResult doCheckout(
            Long userId,
            Long paymentAppId,
            String idempotencyKey,
            String requestFingerprint
    ) {
        OrderIdempotency idempotency = orderIdempotencyService.requireForUpdate(
                userId,
                idempotencyKey,
                requestFingerprint
        );
        if ("COMPLETED".equals(idempotency.getStatus())) {
            OrderInfo existing = orderInfoMapper.selectById(idempotency.getOrderId());
            if (existing == null || !Objects.equals(userId, existing.getUserId())) {
                throw new ConflictException("订单幂等键关联订单不存在");
            }
            return CheckoutResult.from(existing);
        }

        PaymentAppConfig appConfig = paymentConfigLoader.getRequiredAppConfig(paymentAppId);
        Cart cart = cartMapper.selectByUserIdForUpdate(userId);
        if (cart == null) {
            throw new BizException("购物车为空");
        }
        List<CartItem> cartItems = cartItemMapper.selectByCartId(cart.getId());
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BizException("购物车为空");
        }
        for (CartItem cartItem : cartItems) {
            if (cartItem.getProductId() == null) {
                throw new ConflictException("购物车商品无效，请刷新后重试");
            }
            if (cartItem.getQuantity() == null
                    || cartItem.getQuantity() < 1
                    || cartItem.getQuantity() > 99) {
                throw new ConflictException("购物车商品数量无效，请刷新后重试");
            }
        }

        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .distinct()
                .sorted()
                .collect(Collectors.toList());
        Map<Long, Product> productMap = new HashMap<>();
        for (Long productId : productIds) {
            Product product = productMapper.selectByIdForUpdate(productId);
            if (product == null) {
                throw new ConflictException("购物车中存在已删除商品，请刷新后重试");
            }
            productMap.put(product.getId(), product);
        }

        String channelCode = appConfig.getChannelCode();
        String paymentType = resolvePaymentType(channelCode);
        long total = 0L;
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            if (product.getStatus() != ProductStatus.ON_SHELF) {
                throw new ConflictException("购物车中存在已下架商品，请刷新后重试");
            }
            if (product.getAvailableStock() == null
                    || product.getAvailableStock() < cartItem.getQuantity()) {
                throw new ConflictException("购物车中存在库存不足商品，请调整数量后重试");
            }
            if (product.getPrice() == null || product.getPrice() < 0) {
                throw new BizException("课程价格无效");
            }
            long subtotal = Math.multiplyExact((long) product.getPrice(), cartItem.getQuantity());
            total = Math.addExact(total, subtotal);
        }
        if (total > Integer.MAX_VALUE) {
            throw new BizException("订单金额超出系统限制");
        }

        OrderInfo order = new OrderInfo();
        order.setTitle(cartItems.size() == 1
                ? productMap.get(cartItems.get(0).getProductId()).getTitle()
                : productMap.get(cartItems.get(0).getProductId()).getTitle()
                        + "等" + cartItems.size() + "种课程");
        order.setOrderNo(OrderNoUtils.getOrderNo());
        order.setUserId(userId);
        order.setProductId(null);
        order.setTotalFee((int) total);
        order.setOrderStatus(OrderStatus.NOTPAY.getType());
        order.setPaymentType(paymentType);
        order.setPaymentAppId(appConfig.getAppId());
        order.setPaymentChannelCode(channelCode);
        order.setCheckoutRequestId(idempotencyKey);
        order.setVersion(0);

        if (orderInfoMapper.insert(order) != 1 || order.getId() == null) {
            throw new BizException("订单创建失败");
        }

        List<OrderItem> orderItems = new ArrayList<>();
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductTitle(product.getTitle());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(product.getPrice() * cartItem.getQuantity());
            orderItem.setInventoryStatus(InventoryStatus.RESERVED);
            orderItem.setRefundedQuantity(0);
            if (orderItemMapper.insert(orderItem) != 1) {
                throw new BizException("订单明细创建失败");
            }
            orderItems.add(orderItem);
        }
        inventoryService.reserve(order, orderItems);
        closeMessageService.sendCloseOrderMessage(order.getOrderNo(), order.getPaymentType());
        cartItemMapper.deleteByCartId(cart.getId());
        orderIdempotencyService.complete(idempotency.getId(), requestFingerprint, order.getId());
        return CheckoutResult.from(order);
    }

    private String resolvePaymentType(String channelCode) {
        if (PaymentConfigLoader.CHANNEL_WXPAY.equals(channelCode)) {
            return PayType.WXPAY.getType();
        }
        if (PaymentConfigLoader.CHANNEL_ALIPAY.equals(channelCode)) {
            return PayType.ALIPAY.getType();
        }
        throw new BizException("暂不支持的支付渠道：" + channelCode);
    }

}
