package cc.ivera.userdemo.controller;

import cc.ivera.userdemo.controller.dto.LoginReq;
import cc.ivera.userdemo.controller.dto.RegisterReq;
import cc.ivera.userdemo.service.UserFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {

  private final UserFacadeService userService;

  @PostMapping("/register")
  public Map<String, Object> register(@Valid @RequestBody RegisterReq req) throws Exception {
    String uid = userService.register(req.getPhone(), req.getEmail(), req.getPassword());
    return Map.of("uid", uid);
  }

  @PostMapping("/login")
  public Map<String, Object> login(@Valid @RequestBody LoginReq req) throws Exception {
    String token = userService.login(req.getAccount(), req.getPassword());
    return Map.of("token", token, "uid", token);
  }
}
