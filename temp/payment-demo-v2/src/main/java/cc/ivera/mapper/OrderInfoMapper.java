package cc.ivera.mapper;

import cc.ivera.entity.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    OrderInfo selectByOrderNoForUpdate(@Param("orderNo") String orderNo);
}
