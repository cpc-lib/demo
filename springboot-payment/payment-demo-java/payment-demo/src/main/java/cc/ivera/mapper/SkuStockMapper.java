package cc.ivera.mapper;

import cc.ivera.entity.SkuStock;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface SkuStockMapper extends BaseMapper<SkuStock> {

    SkuStock selectBySkuIdForUpdate(@Param("skuId") Long skuId);

    int reserveStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    int commitLockedStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    int releaseLockedStock(@Param("skuId") Long skuId, @Param("quantity") Integer quantity);

    int updateAvailableStock(
            @Param("skuId") Long skuId,
            @Param("availableStock") Integer availableStock,
            @Param("version") Long version
    );
}

