package cc.ivera.mapper;


import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import cc.ivera.model.pojo.Order;

public interface OrderMapper extends BaseMapper<Order> {
    void save(Order order);
}