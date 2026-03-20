package com.example.orderdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderdemo.domain.entity.OrderItemEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderItemMapper extends BaseMapper<OrderItemEntity> {}
