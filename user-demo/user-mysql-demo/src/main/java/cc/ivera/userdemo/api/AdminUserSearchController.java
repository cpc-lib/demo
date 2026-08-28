package cc.ivera.userdemo.api;

import cc.ivera.userdemo.api.dto.SearchUserRequest;
import cc.ivera.userdemo.api.dto.SearchUserResponse;
import cc.ivera.userdemo.service.UserSearchService;
import javax.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/admin/users")
public class AdminUserSearchController {

  private final UserSearchService userSearchService;

  @PostMapping("/search")
  public SearchUserResponse search(@Valid @RequestBody SearchUserRequest req) {
    return userSearchService.search(req);
  }
}
