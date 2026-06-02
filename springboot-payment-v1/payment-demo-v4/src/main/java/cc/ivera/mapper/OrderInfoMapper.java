package cc.ivera.mapper;

import cc.ivera.entity.OrderInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

public interface OrderInfoMapper extends BaseMapper<OrderInfo> {

    /**
     * 按订单号查询并加行级排他锁。
     */
    OrderInfo selectByOrderNoForUpdate(@Param("orderNo") String orderNo);

    /**
     * 查询指定商品 + 支付方式下最新的一笔未支付订单，并加行级排他锁。
     *
     * 用途：创建订单时配合 Redis 分布式锁，避免并发场景重复创建未支付订单。
     */
    OrderInfo selectNoPayOrderForUpdate(@Param("productId") Long productId,
                                        @Param("paymentType") String paymentType,
                                        @Param("orderStatus") String orderStatus);
}
