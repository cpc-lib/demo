package cc.ivera.api;

import cc.ivera.api.dto.CreateUserRequest;
import cc.ivera.api.dto.UpdateUserRequest;
import cc.ivera.api.dto.UserPageQuery;
import cc.ivera.api.dto.UserResponse;
import cc.ivera.common.ApiResponse;
import cc.ivera.common.PageResponse;
import cc.ivera.service.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@Validated
public class UserController {

    private final UserService service;

    @PostMapping
    public ApiResponse<UserResponse> create(@Valid @RequestBody CreateUserRequest req) {
        return ApiResponse.ok(service.create(req));
    }

    @GetMapping("/{id}")
    public ApiResponse<UserResponse> get(@PathVariable String id) {
        return ApiResponse.ok(service.get(id));
    }

    @PutMapping("/{id}")
    public ApiResponse<UserResponse> update(@PathVariable String id, @Valid @RequestBody UpdateUserRequest req) {
        return ApiResponse.ok(service.update(id, req));
    }

    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable String id) {
        service.delete(id);
        return ApiResponse.ok(null);
    }

    /**
     * Production-grade paging query:
     * GET /api/users?page=0&size=10&keyword=tom&status=1&sort=updatedAt,desc;uid,asc
     */
    @GetMapping
    public ApiResponse<PageResponse<UserResponse>> page(@Valid UserPageQuery query) {
        return ApiResponse.ok(service.page(query));
    }
}
