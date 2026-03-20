package com.example.orderdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderdemo.domain.entity.OrderEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OrderMapper extends BaseMapper<OrderEntity> {}
