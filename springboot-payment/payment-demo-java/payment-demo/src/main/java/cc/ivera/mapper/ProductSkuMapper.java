package cc.ivera.mapper;

import cc.ivera.entity.ProductSku;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductSkuMapper extends BaseMapper<ProductSku> {

    ProductSku selectDefaultByProductId(@Param("productId") Long productId);

    List<ProductSku> selectDefaultByProductIds(@Param("productIds") List<Long> productIds);
}

