package cc.ivera.controller;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import cc.ivera.domain.User;
import cc.ivera.service.UserService;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;


    @GetMapping("/{id}")
    public User findByIdWithOutCache(@PathVariable("id") Long id) {
        return userService.findByIdWithOutCache(id);
    }

    /*支持缓存的接口*/
    @GetMapping("/hashMap/{id}")
    public User findById_hashMap(@PathVariable("id") Long id){
        return userService.findById_hashMap(id);
    }


    @GetMapping("/ehcache/{id}")
    public User findById_ecache(@PathVariable("id") Long id){
        return userService.findById_ecache(id);
    }

    @GetMapping("/redis/{id}")
    public User findById_redis(@PathVariable("id") Long id){
        return userService.findById_redis(id);
    }

}
