package com.example.points.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.points.entity.UserPoints;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserPointsMapper extends BaseMapper<UserPoints> {
}
