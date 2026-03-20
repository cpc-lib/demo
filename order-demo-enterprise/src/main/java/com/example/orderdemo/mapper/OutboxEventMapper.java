package com.example.orderdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderdemo.domain.entity.OutboxEventEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEventEntity> {}
