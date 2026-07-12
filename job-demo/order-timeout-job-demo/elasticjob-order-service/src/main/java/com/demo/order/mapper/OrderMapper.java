package com.demo.order.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.demo.order.domain.OrderEntity;
import org.apache.ibatis.annotations.*;
import java.util.List;

public interface OrderMapper extends BaseMapper<OrderEntity> {
    @Select("SELECT * FROM t_order WHERE status='UNPAID' AND pay_deadline < NOW() ORDER BY id ASC LIMIT #{limit}")
    List<OrderEntity> selectTimeoutUnpaidOrders(@Param("limit") int limit);

    @Update("UPDATE t_order SET status='CLOSED', close_reason=#{reason}, version=version+1, updated_at=NOW() WHERE id=#{id} AND status='UNPAID' AND version=#{version}")
    int closeByOptimisticLock(@Param("id") Long id, @Param("version") Integer version, @Param("reason") String reason);
}
