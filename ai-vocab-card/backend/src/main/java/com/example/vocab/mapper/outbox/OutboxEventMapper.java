package com.example.vocab.mapper.outbox;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.vocab.entity.outbox.OutboxEvent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface OutboxEventMapper extends BaseMapper<OutboxEvent> {}
