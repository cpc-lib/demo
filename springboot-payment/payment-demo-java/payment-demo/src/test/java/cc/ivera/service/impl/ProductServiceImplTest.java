package cc.ivera.service.impl;

import cc.ivera.entity.Product;
import cc.ivera.mapper.ProductMapper;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class ProductServiceImplTest {

    @Test
    void publicCatalogUsesOnShelfQueryWithoutDroppingSoldOutProducts() {
        ProductMapper productMapper = mock(ProductMapper.class);
        List<Product> onShelfProducts = Arrays.asList(new Product(), new Product());
        when(productMapper.selectOnShelf()).thenReturn(onShelfProducts);
        ProductServiceImpl service = new ProductServiceImpl(productMapper);

        List<Product> result = service.listPublicSaleable();

        assertSame(onShelfProducts, result);
        verify(productMapper).selectOnShelf();
    }
}
