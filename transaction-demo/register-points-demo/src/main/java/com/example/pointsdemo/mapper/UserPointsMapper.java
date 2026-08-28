package com.example.pointsdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.pointsdemo.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPointsMapper extends BaseMapper<UserPoints> {
}