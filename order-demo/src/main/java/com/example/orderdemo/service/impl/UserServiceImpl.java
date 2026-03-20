package com.example.orderdemo.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.orderdemo.common.ApiException;
import com.example.orderdemo.domain.dto.CreateUserRequest;
import com.example.orderdemo.domain.entity.User;
import com.example.orderdemo.mapper.UserMapper;
import com.example.orderdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserServiceImpl implements UserService {

  private final UserMapper userMapper;

  @Override
  @Transactional
  public Long createUser(CreateUserRequest req) {
    Long cnt = userMapper.selectCount(new LambdaQueryWrapper<User>()
        .eq(User::getUsername, req.getUsername()));
    if (cnt != null && cnt > 0) {
      throw ApiException.conflict("username 已存在");
    }

    User u = new User();
    u.setUsername(req.getUsername());
    u.setPhone(req.getPhone());
    u.setEmail(req.getEmail());
    u.setStatus(1);

    userMapper.insert(u);
    return u.getId();
  }

  @Override
  public User getByIdOrThrow(Long id) {
    User u = userMapper.selectById(id);
    if (u == null) {
      throw ApiException.notFound("用户不存在: " + id);
    }
    return u;
  }
}
