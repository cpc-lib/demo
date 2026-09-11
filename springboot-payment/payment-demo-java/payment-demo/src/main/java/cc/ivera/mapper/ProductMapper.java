package cc.ivera.mapper;

import cc.ivera.entity.Product;
import cc.ivera.enums.ProductStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ProductMapper extends BaseMapper<Product> {

    Product selectByIdForUpdate(@Param("id") Long id);

    Product selectByIdWithDefaultStock(@Param("id") Long id);

    List<Product> selectByIdsWithDefaultStock(@Param("ids") List<Long> ids);

    List<Product> selectAllWithDefaultStock();

    List<Product> selectOnShelf();

    int updateBasicInfo(
            @Param("id") Long id,
            @Param("title") String title,
            @Param("price") Integer price,
            @Param("version") Integer version
    );

    int updateStatus(
            @Param("id") Long id,
            @Param("status") ProductStatus status,
            @Param("version") Integer version
    );

}
