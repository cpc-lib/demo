package com.demo.seata.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.seata.order.entity.OrderInfo;
import org.apache.ibatis.annotations.Mapper;

/**
 * 订单Mapper
 */
@Mapper
public interface OrderInfoMapper extends BaseMapper<OrderInfo> {
}
