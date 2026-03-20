package com.example.orderdemo.service;

import com.example.orderdemo.domain.dto.CreateUserRequest;
import com.example.orderdemo.domain.entity.User;

public interface UserService {
  Long createUser(CreateUserRequest req);
  User getByIdOrThrow(Long id);
}
