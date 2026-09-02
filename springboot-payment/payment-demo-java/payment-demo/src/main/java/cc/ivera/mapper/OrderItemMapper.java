package cc.ivera.mapper;

import cc.ivera.entity.OrderItem;
import cc.ivera.enums.InventoryStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;
import org.apache.ibatis.annotations.Update;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("select * from t_order_item where order_id = #{orderId} order by id")
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);

    @Select("select * from t_order_item where order_id = #{orderId} order by product_id, id for update")
    List<OrderItem> selectByOrderIdForUpdate(@Param("orderId") Long orderId);

    @Update("update t_order_item set inventory_status = #{targetStatus} "
            + "where id = #{id} and inventory_status = #{currentStatus}")
    int updateInventoryStatus(
            @Param("id") Long id,
            @Param("currentStatus") InventoryStatus currentStatus,
            @Param("targetStatus") InventoryStatus targetStatus
    );
}
