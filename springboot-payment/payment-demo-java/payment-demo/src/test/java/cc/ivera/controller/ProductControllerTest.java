package cc.ivera.controller;

import cc.ivera.entity.Product;
import cc.ivera.enums.ProductStatus;
import cc.ivera.service.ProductService;
import cc.ivera.vo.R;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

class ProductControllerTest {

    @Test
    @SuppressWarnings("unchecked")
    void publicCatalogReturnsSoldOutFlagWithoutLeakingAdminInventoryFields() {
        ProductService productService = mock(ProductService.class);
        Product available = product(1L, 5, 2, 3, 4);
        Product soldOut = product(2L, 0, 6, 7, 8);
        when(productService.listPublicSaleable()).thenReturn(Arrays.asList(available, soldOut));
        ProductController controller = new ProductController(productService);

        R<Map<String, Object>> response = controller.list();
        List<Map<String, Object>> products =
                (List<Map<String, Object>>) response.getData().get("productList");

        assertEquals(2, products.size());
        assertEquals(1L, products.get(0).get("id"));
        assertEquals("商品1", products.get(0).get("title"));
        assertEquals(1000, products.get(0).get("price"));
        assertEquals(5, products.get(0).get("availableStock"));
        assertEquals(Boolean.TRUE, products.get(0).get("saleable"));
        assertEquals(Boolean.FALSE, products.get(1).get("saleable"));
        assertFalse(products.get(0).containsKey("status"));
        assertFalse(products.get(0).containsKey("lockedStock"));
        assertFalse(products.get(0).containsKey("soldStock"));
        assertFalse(products.get(0).containsKey("version"));
        assertTrue(response.getData().containsKey("productList"));
    }

    @Test
    @SuppressWarnings("unchecked")
    void saleableFlagAlsoRequiresOnShelfStatus() {
        ProductService productService = mock(ProductService.class);
        Product offShelf = product(3L, 5, 0, 0, 0);
        offShelf.setStatus(ProductStatus.OFF_SHELF);
        when(productService.listPublicSaleable()).thenReturn(Arrays.asList(offShelf));
        ProductController controller = new ProductController(productService);

        List<Map<String, Object>> products = (List<Map<String, Object>>)
                controller.list().getData().get("productList");

        assertEquals(Boolean.FALSE, products.get(0).get("saleable"));
    }

    private Product product(Long id, int available, int locked, int sold, int version) {
        Product product = new Product();
        product.setId(id);
        product.setTitle("商品" + id);
        product.setPrice(1000);
        product.setStatus(ProductStatus.ON_SHELF);
        product.setAvailableStock(available);
        product.setLockedStock(locked);
        product.setSoldStock(sold);
        product.setVersion(version);
        return product;
    }
}
