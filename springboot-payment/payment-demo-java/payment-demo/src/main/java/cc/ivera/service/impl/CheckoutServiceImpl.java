package cc.ivera.service.impl;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.Cart;
import cc.ivera.entity.CartItem;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;
import cc.ivera.entity.Product;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.CartItemMapper;
import cc.ivera.mapper.CartMapper;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.OrderItemMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.CheckoutService;
import cc.ivera.service.OrderCloseMessageService;
import cc.ivera.util.OrderNoUtils;
import cc.ivera.vo.CheckoutResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionSynchronizationAdapter;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;
import org.springframework.util.StringUtils;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CheckoutServiceImpl implements CheckoutService {

    private static final long CHECKOUT_LOCK_WAIT_MS = 3000L;

    private static final long CHECKOUT_LOCK_LEASE_MS = 10000L;

    private final CartMapper cartMapper;

    private final CartItemMapper cartItemMapper;

    private final ProductMapper productMapper;

    private final OrderInfoMapper orderInfoMapper;

    private final OrderItemMapper orderItemMapper;

    private final PaymentConfigLoader paymentConfigLoader;

    private final OrderCloseMessageService closeMessageService;

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
        this.lockTemplate = lockTemplate;
        this.transactionTemplate = transactionTemplate;
    }

    @Override
    public CheckoutResult checkout(Long userId, Long paymentAppId, String checkoutRequestId) {
        if (userId == null) {
            throw new BizException("用户ID不能为空");
        }
        if (paymentAppId == null) {
            throw new BizException("支付应用不能为空");
        }
        if (!StringUtils.hasText(checkoutRequestId) || checkoutRequestId.length() > 64) {
            throw new BizException("结算请求号不能为空且不能超过64个字符");
        }

        OrderInfo existing = orderInfoMapper.selectByCheckoutKey(userId, checkoutRequestId);
        if (existing != null) {
            return CheckoutResult.from(existing);
        }

        PaymentAppConfig appConfig = paymentConfigLoader.getRequiredAppConfig(paymentAppId);
        return lockTemplate.execute(
                "payment:cart:checkout:" + userId,
                CHECKOUT_LOCK_WAIT_MS,
                CHECKOUT_LOCK_LEASE_MS,
                () -> transactionTemplate.execute(status -> doCheckout(
                        userId,
                        checkoutRequestId,
                        appConfig
                ))
        );
    }

    private CheckoutResult doCheckout(
            Long userId,
            String checkoutRequestId,
            PaymentAppConfig appConfig
    ) {
        OrderInfo existing = orderInfoMapper.selectByCheckoutKey(userId, checkoutRequestId);
        if (existing != null) {
            return CheckoutResult.from(existing);
        }

        Cart cart = cartMapper.selectByUserIdForUpdate(userId);
        if (cart == null) {
            throw new BizException("购物车为空");
        }
        List<CartItem> cartItems = cartItemMapper.selectByCartId(cart.getId());
        if (cartItems == null || cartItems.isEmpty()) {
            throw new BizException("购物车为空");
        }

        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = new HashMap<>();
        for (Product product : products) {
            productMap.put(product.getId(), product);
        }
        if (productMap.size() != cartItems.size()) {
            throw new BizException("购物车中存在已下架课程，请刷新后重试");
        }

        String channelCode = appConfig.getChannelCode();
        String paymentType = resolvePaymentType(channelCode);
        long total = 0L;
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
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
        order.setCheckoutRequestId(checkoutRequestId);
        order.setVersion(0);

        try {
            orderInfoMapper.insert(order);
        } catch (DuplicateKeyException ex) {
            OrderInfo duplicate = orderInfoMapper.selectByCheckoutKey(userId, checkoutRequestId);
            if (duplicate != null) {
                return CheckoutResult.from(duplicate);
            }
            throw ex;
        }

        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            OrderItem orderItem = new OrderItem();
            orderItem.setOrderId(order.getId());
            orderItem.setProductId(product.getId());
            orderItem.setProductTitle(product.getTitle());
            orderItem.setUnitPrice(product.getPrice());
            orderItem.setQuantity(cartItem.getQuantity());
            orderItem.setSubtotal(product.getPrice() * cartItem.getQuantity());
            orderItemMapper.insert(orderItem);
        }
        cartItemMapper.deleteByCartId(cart.getId());
        sendCloseMessageAfterCommit(order);
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

    private void sendCloseMessageAfterCommit(OrderInfo order) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            closeMessageService.sendCloseOrderMessage(order.getOrderNo(), order.getPaymentType());
            return;
        }
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronizationAdapter() {
            @Override
            public void afterCommit() {
                closeMessageService.sendCloseOrderMessage(order.getOrderNo(), order.getPaymentType());
            }
        });
    }
}
