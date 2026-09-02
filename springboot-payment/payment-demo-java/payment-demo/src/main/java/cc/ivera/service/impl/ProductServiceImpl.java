package cc.ivera.service.impl;

import cc.ivera.entity.Product;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.service.ProductService;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class ProductServiceImpl extends ServiceImpl<ProductMapper, Product> implements ProductService {

    private final ProductMapper productMapper;

    public ProductServiceImpl(ProductMapper productMapper) {
        this.productMapper = productMapper;
    }

    @Override
    public List<Product> listPublicSaleable() {
        return productMapper.selectOnShelf();
    }
}
