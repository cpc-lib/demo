package cc.ivera.userdemo.api;

import cc.ivera.userdemo.api.dto.LoginRequest;
import cc.ivera.userdemo.api.dto.LoginResponse;
import cc.ivera.userdemo.service.AuthService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/auth")
public class AuthController {

  private final AuthService authService;

  @PostMapping("/login")
  public LoginResponse login(@Valid @RequestBody LoginRequest req) {
    return authService.login(req);
  }
}
