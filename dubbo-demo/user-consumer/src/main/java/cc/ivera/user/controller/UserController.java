package cc.ivera.user.controller;


import cc.ivera.user.domain.User;
import cc.ivera.user.service.UserService;
import lombok.extern.slf4j.Slf4j;
import org.apache.dubbo.config.annotation.DubboReference;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@Slf4j
@RestController
@RequestMapping("/user")
public class UserController {

    @DubboReference(version = "2.0.0",retries = 0,loadbalance = "Random")
    private UserService userService;

    /**
     * 路径： /user/110
     * @param id 用户id
     * @return 用户
     */
    @GetMapping("/usernames/{id}")
    public String queryUsername(@PathVariable("id") Long id) {
        return userService.queryUsername(id);
    }


    /**
     * 根据id查询用户
     */
    @GetMapping("/{id}")
    public User queryById(@PathVariable("id") Long id) {
        User user  = userService.queryById(id);
        return user;
    }
}
