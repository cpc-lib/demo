package com.example.vocab.mapper.auth;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.vocab.entity.auth.AppUser;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface AppUserMapper extends BaseMapper<AppUser> {}
