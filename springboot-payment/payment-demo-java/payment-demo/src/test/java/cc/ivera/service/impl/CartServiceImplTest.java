package cc.ivera.service.impl;

import cc.ivera.entity.Cart;
import cc.ivera.entity.CartItem;
import cc.ivera.entity.Product;
import cc.ivera.exception.BizException;
import cc.ivera.mapper.CartItemMapper;
import cc.ivera.mapper.CartMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.vo.CartView;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.Arrays;
import java.util.Collections;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class CartServiceImplTest {

    private CartMapper cartMapper;
    private CartItemMapper cartItemMapper;
    private ProductMapper productMapper;
    private CartServiceImpl cartService;

    @BeforeEach
    void setUp() {
        cartMapper = mock(CartMapper.class);
        cartItemMapper = mock(CartItemMapper.class);
        productMapper = mock(ProductMapper.class);
        cartService = new CartServiceImpl(cartMapper, cartItemMapper, productMapper);
        when(cartMapper.selectByUserIdForUpdate(4L)).thenReturn(cart(30L, 4L));
    }

    @Test
    void addNewCoursePersistsRequestedQuantity() {
        when(productMapper.selectById(101L)).thenReturn(product(101L, "Java", 19900));
        when(cartItemMapper.selectByCartAndProduct(30L, 101L)).thenReturn(null);
        when(cartItemMapper.countByCartId(30L)).thenReturn(0);

        cartService.addItem(4L, 101L, 3);

        ArgumentCaptor<CartItem> captor = ArgumentCaptor.forClass(CartItem.class);
        verify(cartItemMapper).insert(captor.capture());
        assertEquals(30L, captor.getValue().getCartId());
        assertEquals(101L, captor.getValue().getProductId());
        assertEquals(3, captor.getValue().getQuantity());
    }

    @Test
    void addingExistingCourseAggregatesQuantity() {
        when(productMapper.selectById(101L)).thenReturn(product(101L, "Java", 19900));
        CartItem existing = item(8L, 30L, 101L, 2);
        when(cartItemMapper.selectByCartAndProduct(30L, 101L)).thenReturn(existing);

        cartService.addItem(4L, 101L, 4);

        assertEquals(6, existing.getQuantity());
        verify(cartItemMapper).updateById(existing);
    }

    @Test
    void aggregateAboveNinetyNineIsRejected() {
        when(productMapper.selectById(101L)).thenReturn(product(101L, "Java", 19900));
        when(cartItemMapper.selectByCartAndProduct(30L, 101L))
                .thenReturn(item(8L, 30L, 101L, 98));

        assertThrows(BizException.class, () -> cartService.addItem(4L, 101L, 2));

        verify(cartItemMapper, never()).updateById(any(CartItem.class));
    }

    @Test
    void twentyCourseLimitRejectsAnotherDistinctCourse() {
        when(productMapper.selectById(202L)).thenReturn(product(202L, "Vue", 9900));
        when(cartItemMapper.selectByCartAndProduct(30L, 202L)).thenReturn(null);
        when(cartItemMapper.countByCartId(30L)).thenReturn(20);

        assertThrows(BizException.class, () -> cartService.addItem(4L, 202L, 1));

        verify(cartItemMapper, never()).insert(any(CartItem.class));
    }

    @Test
    void missingCourseAndInvalidQuantityAreRejected() {
        assertThrows(BizException.class, () -> cartService.addItem(4L, 101L, 0));
        assertThrows(BizException.class, () -> cartService.addItem(4L, 101L, 100));
        when(productMapper.selectById(404L)).thenReturn(null);
        assertThrows(BizException.class, () -> cartService.addItem(4L, 404L, 1));
    }

    @Test
    void cartViewUsesCurrentPricesAndCalculatesTotals() {
        when(cartMapper.selectByUserId(4L)).thenReturn(cart(30L, 4L));
        when(cartItemMapper.selectByCartId(30L)).thenReturn(Arrays.asList(
                item(1L, 30L, 101L, 2),
                item(2L, 30L, 202L, 3)
        ));
        when(productMapper.selectBatchIds(any())).thenReturn(Arrays.asList(
                product(101L, "Java", 1000),
                product(202L, "Vue", 2500)
        ));

        CartView view = cartService.getCart(4L);

        assertEquals(2, view.getDistinctCount());
        assertEquals(5, view.getTotalQuantity());
        assertEquals(9500, view.getTotalAmount());
        assertEquals(2000, view.getItems().get(0).getSubtotal());
    }

    @Test
    void userWithoutCartGetsAnEmptyViewWithoutCreatingDatabaseState() {
        when(cartMapper.selectByUserId(5L)).thenReturn(null);

        CartView view = cartService.getCart(5L);

        assertEquals(Collections.emptyList(), view.getItems());
        assertEquals(0, view.getTotalAmount());
        verify(cartMapper, never()).insert(any(Cart.class));
    }

    @Test
    void cartTotalOverflowIsRejectedInsteadOfReturningWrappedAmount() {
        when(cartMapper.selectByUserId(4L)).thenReturn(cart(30L, 4L));
        when(cartItemMapper.selectByCartId(30L)).thenReturn(Arrays.asList(
                item(1L, 30L, 101L, 2),
                item(2L, 30L, 202L, 2)
        ));
        when(productMapper.selectBatchIds(any())).thenReturn(Arrays.asList(
                product(101L, "Java", Integer.MAX_VALUE),
                product(202L, "Vue", Integer.MAX_VALUE)
        ));

        assertThrows(BizException.class, () -> cartService.getCart(4L));
    }

    private Cart cart(Long id, Long userId) {
        Cart cart = new Cart();
        cart.setId(id);
        cart.setUserId(userId);
        return cart;
    }

    private CartItem item(Long id, Long cartId, Long productId, int quantity) {
        CartItem item = new CartItem();
        item.setId(id);
        item.setCartId(cartId);
        item.setProductId(productId);
        item.setQuantity(quantity);
        return item;
    }

    private Product product(Long id, String title, int price) {
        Product product = new Product();
        product.setId(id);
        product.setTitle(title);
        product.setPrice(price);
        return product;
    }
}
