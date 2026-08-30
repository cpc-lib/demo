package cc.ivera.controller;

import cc.ivera.dto.CartItemRequest;
import cc.ivera.dto.CartQuantityRequest;
import cc.ivera.security.AuthContext;
import cc.ivera.service.CartService;
import cc.ivera.vo.CartView;
import cc.ivera.vo.R;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/cart")
public class CartController {

    private final CartService cartService;

    public CartController(CartService cartService) {
        this.cartService = cartService;
    }

    @GetMapping
    public R<CartView> getCart() {
        return R.ok(cartService.getCart(currentUserId()));
    }

    @PostMapping("/items")
    public R<Void> addItem(@Valid @RequestBody CartItemRequest request) {
        cartService.addItem(currentUserId(), request.getProductId(), request.getQuantity());
        return R.ok(null);
    }

    @PutMapping("/items/{productId}")
    public R<Void> updateItem(
            @PathVariable Long productId,
            @Valid @RequestBody CartQuantityRequest request
    ) {
        cartService.updateItem(currentUserId(), productId, request.getQuantity());
        return R.ok(null);
    }

    @DeleteMapping("/items/{productId}")
    public R<Void> removeItem(@PathVariable Long productId) {
        cartService.removeItem(currentUserId(), productId);
        return R.ok(null);
    }

    @DeleteMapping
    public R<Void> clear() {
        cartService.clear(currentUserId());
        return R.ok(null);
    }

    private Long currentUserId() {
        return AuthContext.requireUser().getUserId();
    }
}
