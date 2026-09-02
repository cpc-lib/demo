package cc.ivera.mapper;

import cc.ivera.entity.InventoryOperation;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

public interface InventoryOperationMapper extends BaseMapper<InventoryOperation> {

    @Select("select * from t_inventory_operation where business_key = #{businessKey} limit 1")
    InventoryOperation selectByBusinessKey(@Param("businessKey") String businessKey);

    @Select("select * from t_inventory_operation where business_key = #{businessKey} limit 1 for update")
    InventoryOperation selectByBusinessKeyForUpdate(@Param("businessKey") String businessKey);

    @Select("select * from t_inventory_operation where product_id = #{productId} order by create_time desc, id desc")
    List<InventoryOperation> selectByProductIdOrderByCreateTimeDesc(@Param("productId") Long productId);
}
