package com.example.points.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.points.entity.MsgIdempotent;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface MsgIdempotentMapper extends BaseMapper<MsgIdempotent> {
}
