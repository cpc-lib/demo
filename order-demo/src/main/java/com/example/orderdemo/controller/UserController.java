package com.example.orderdemo.controller;

import com.example.orderdemo.common.Result;
import com.example.orderdemo.domain.dto.CreateUserRequest;
import com.example.orderdemo.domain.entity.User;
import com.example.orderdemo.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

  private final UserService userService;

  @PostMapping
  public Result<Map<String, Object>> create(@Valid @RequestBody CreateUserRequest req) {
    Long id = userService.createUser(req);
    return Result.ok(Map.of("id", id));
  }

  @GetMapping("/{id}")
  public Result<User> get(@PathVariable Long id) {
    return Result.ok(userService.getByIdOrThrow(id));
  }
}
