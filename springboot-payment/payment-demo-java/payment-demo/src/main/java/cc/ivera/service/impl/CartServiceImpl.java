package cc.ivera.service.impl;

import cc.ivera.entity.Cart;
import cc.ivera.entity.CartItem;
import cc.ivera.entity.Product;
import cc.ivera.enums.ProductStatus;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.NotFoundException;
import cc.ivera.mapper.CartItemMapper;
import cc.ivera.mapper.CartMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.CartService;
import cc.ivera.vo.CartItemView;
import cc.ivera.vo.CartView;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Service
public class CartServiceImpl implements CartService {

    private static final int MAX_QUANTITY = 99;

    private static final int MAX_DISTINCT_PRODUCTS = 20;

    private final CartMapper cartMapper;

    private final CartItemMapper cartItemMapper;

    private final ProductMapper productMapper;

    public CartServiceImpl(CartMapper cartMapper, CartItemMapper cartItemMapper, ProductMapper productMapper) {
        this.cartMapper = cartMapper;
        this.cartItemMapper = cartItemMapper;
        this.productMapper = productMapper;
    }

    @Override
    public CartView getCart(Long userId) {
        Cart cart = cartMapper.selectByUserId(userId);
        if (cart == null) {
            return emptyCart();
        }
        List<CartItem> cartItems = cartItemMapper.selectByCartId(cart.getId());
        if (cartItems == null || cartItems.isEmpty()) {
            return emptyCart();
        }

        List<Long> productIds = cartItems.stream()
                .map(CartItem::getProductId)
                .collect(Collectors.toList());
        List<Product> products = productMapper.selectBatchIds(productIds);
        Map<Long, Product> productMap = new HashMap<>();
        for (Product product : products) {
            productMap.put(product.getId(), product);
        }

        List<CartItemView> views = new ArrayList<>();
        int totalQuantity = 0;
        long totalAmount = 0L;
        for (CartItem cartItem : cartItems) {
            Product product = productMap.get(cartItem.getProductId());
            if (product == null) {
                continue;
            }
            if (product.getPrice() == null || product.getPrice() < 0) {
                throw new BizException("课程价格无效");
            }
            long subtotal = Math.multiplyExact((long) product.getPrice(), cartItem.getQuantity());
            totalAmount = Math.addExact(totalAmount, subtotal);
            if (subtotal > Integer.MAX_VALUE || totalAmount > Integer.MAX_VALUE) {
                throw new BizException("购物车金额超出系统限制");
            }
            String unavailableReason = unavailableReason(product, cartItem.getQuantity());
            views.add(new CartItemView(
                    product.getId(),
                    product.getTitle(),
                    product.getPrice(),
                    cartItem.getQuantity(),
                    (int) subtotal,
                    product.getStatus(),
                    product.getAvailableStock(),
                    unavailableReason == null,
                    unavailableReason
            ));
            totalQuantity += cartItem.getQuantity();
        }
        return new CartView(views, views.size(), totalQuantity, (int) totalAmount);
    }

    @Override
    @Transactional
    public void addItem(Long userId, Long productId, Integer quantity) {
        validateQuantity(quantity);
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        requirePurchasable(product, quantity);

        Cart cart = getOrCreateCartForUpdate(userId);
        CartItem existing = cartItemMapper.selectByCartAndProduct(cart.getId(), productId);
        if (existing != null) {
            int targetQuantity = existing.getQuantity() + quantity;
            validateQuantity(targetQuantity);
            requirePurchasable(product, targetQuantity);
            existing.setQuantity(targetQuantity);
            cartItemMapper.updateById(existing);
            return;
        }
        if (cartItemMapper.countByCartId(cart.getId()) >= MAX_DISTINCT_PRODUCTS) {
            throw new BizException("购物车最多选择20种课程");
        }

        CartItem cartItem = new CartItem();
        cartItem.setCartId(cart.getId());
        cartItem.setProductId(productId);
        cartItem.setQuantity(quantity);
        cartItemMapper.insert(cartItem);
    }

    @Override
    @Transactional
    public void updateItem(Long userId, Long productId, Integer quantity) {
        validateQuantity(quantity);
        Cart cart = cartMapper.selectByUserIdForUpdate(userId);
        CartItem item = cart == null ? null : cartItemMapper.selectByCartAndProduct(cart.getId(), productId);
        if (item == null) {
            throw new BizException("购物车中不存在该课程");
        }
        Product product = productMapper.selectById(productId);
        if (product == null) {
            throw new NotFoundException("商品不存在");
        }
        requirePurchasable(product, quantity);
        item.setQuantity(quantity);
        cartItemMapper.updateById(item);
    }

    @Override
    @Transactional
    public void removeItem(Long userId, Long productId) {
        Cart cart = cartMapper.selectByUserIdForUpdate(userId);
        if (cart != null) {
            cartItemMapper.deleteByCartAndProduct(cart.getId(), productId);
        }
    }

    @Override
    @Transactional
    public void clear(Long userId) {
        Cart cart = cartMapper.selectByUserIdForUpdate(userId);
        if (cart != null) {
            cartItemMapper.deleteByCartId(cart.getId());
        }
    }

    private Cart getOrCreateCartForUpdate(Long userId) {
        Cart cart = cartMapper.selectByUserIdForUpdate(userId);
        if (cart != null) {
            return cart;
        }
        Cart created = new Cart();
        created.setUserId(userId);
        created.setVersion(0);
        cartMapper.insert(created);
        return created;
    }

    private void validateQuantity(Integer quantity) {
        if (quantity == null || quantity < 1 || quantity > MAX_QUANTITY) {
            throw new BizException("课程数量必须在1到99之间");
        }
    }

    private void requirePurchasable(Product product, int quantity) {
        String reason = unavailableReason(product, quantity);
        if ("OFF_SHELF".equals(reason)) {
            throw new ConflictException("商品已下架");
        }
        if ("SOLD_OUT".equals(reason)) {
            throw new ConflictException("商品已售罄");
        }
        if ("INSUFFICIENT_STOCK".equals(reason)) {
            throw new ConflictException("商品可用库存不足");
        }
    }

    private String unavailableReason(Product product, int quantity) {
        if (!ProductStatus.ON_SHELF.equals(product.getStatus())) {
            return "OFF_SHELF";
        }
        if (product.getAvailableStock() == null || product.getAvailableStock() <= 0) {
            return "SOLD_OUT";
        }
        if (quantity > product.getAvailableStock()) {
            return "INSUFFICIENT_STOCK";
        }
        return null;
    }

    private CartView emptyCart() {
        return new CartView(Collections.emptyList(), 0, 0, 0);
    }
}
