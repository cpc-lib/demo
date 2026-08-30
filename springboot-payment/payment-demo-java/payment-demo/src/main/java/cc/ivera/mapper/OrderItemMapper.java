package cc.ivera.mapper;

import cc.ivera.entity.OrderItem;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Select;

import java.util.List;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItem> {

    @Select("select * from t_order_item where order_id = #{orderId} order by id")
    List<OrderItem> selectByOrderId(@Param("orderId") Long orderId);
}
