package cc.ivera.userdemo.controller;

import cc.ivera.userdemo.controller.dto.UpdateContactReq;
import cc.ivera.userdemo.service.UserFacadeService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import java.util.Map;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
public class UserController {

  private final UserFacadeService userService;

  @PostMapping("/{uid}/update")
  public Map<String, Object> update(@PathVariable String uid, @Valid @RequestBody UpdateContactReq req) throws Exception {
    long ver = userService.updateContact(uid, req.getPhone(), req.getEmail());
    return Map.of("uid", uid, "version", ver);
  }

  @PostMapping("/{uid}/cancel")
  public Map<String, Object> cancel(@PathVariable String uid) throws Exception {
    long ver = userService.cancel(uid);
    return Map.of("uid", uid, "version", ver);
  }
}
