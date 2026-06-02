package cc.ivera.mapper;

import cc.ivera.entity.Product;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface ProductMapper extends BaseMapper<Product> {

    Product selectByIdForUpdate(@Param("id") Long id);
}
