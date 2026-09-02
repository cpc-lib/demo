package cc.ivera.mapper;

import cc.ivera.entity.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 按订单号查询并加行级排他锁。
     */
    OrderInfo selectByOrderNoForUpdate(@Param("orderNo") String orderNo);

}
