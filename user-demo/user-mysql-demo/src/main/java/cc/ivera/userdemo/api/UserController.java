package cc.ivera.userdemo.api;

import cc.ivera.userdemo.api.dto.CreateUserRequest;
import cc.ivera.userdemo.domain.UserBase;
import cc.ivera.userdemo.service.UserWriteService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/users")
public class UserController {

  private final UserWriteService userWriteService;

  @PostMapping
  public UserBase create(@Valid @RequestBody CreateUserRequest req) {
    return userWriteService.create(req);
  }
}
