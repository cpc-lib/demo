package cc.ivera.controller;

import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import cc.ivera.entity.CreateUserRequest;
import cc.ivera.entity.UserProfile;
import cc.ivera.service.UserProfileService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/userProfile")
@RequiredArgsConstructor
public class UserProfileController {

    private final UserProfileService userProfileService;


    @GetMapping("/page")
    public Page<UserProfile> page(
            @RequestParam(defaultValue = "1") Long current,
            @RequestParam(defaultValue = "10") Long size,
            @RequestParam(required = false) String name) {
        return userProfileService.pageQuery(current, size, name);
    }

    @PostMapping("/add")
    public Map<String, Object> add() {
        Long id = userProfileService.addDemoUser();
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        return result;
    }

    @PostMapping("/custom")
    public Map<String, Object> addCustom(@RequestBody CreateUserRequest request) {
        Long id = userProfileService.addCustomUser(request.getName(), request.getHobbies(), request.getTags());
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        return result;
    }

    @GetMapping("/{id}")
    public UserProfile get(@PathVariable Long id) {
        return userProfileService.getById(id);
    }

    @PutMapping("/{id}")
    public Map<String, Object> update(@PathVariable Long id) {
        userProfileService.updateDemoUser(id);
        Map<String, Object> result = new HashMap<>();
        result.put("success", true);
        result.put("id", id);
        return result;
    }


}
