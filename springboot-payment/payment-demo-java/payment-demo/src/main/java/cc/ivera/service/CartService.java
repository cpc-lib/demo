package cc.ivera.service;

import cc.ivera.vo.CartView;

public interface CartService {

    CartView getCart(Long userId);

    void addItem(Long userId, Long productId, Integer quantity);

    void updateItem(Long userId, Long productId, Integer quantity);

    void removeItem(Long userId, Long productId);

    void clear(Long userId);
}
