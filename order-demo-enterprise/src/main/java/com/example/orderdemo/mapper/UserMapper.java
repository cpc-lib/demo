package com.example.orderdemo.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.orderdemo.domain.entity.UserEntity;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface UserMapper extends BaseMapper<UserEntity> {}
