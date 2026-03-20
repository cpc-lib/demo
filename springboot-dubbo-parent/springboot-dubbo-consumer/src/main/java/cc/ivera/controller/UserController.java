package cc.ivera.controller;

import cc.ivera.domain.User;
import cc.ivera.service.UserService;

import com.alibaba.dubbo.config.annotation.Reference;
import com.github.pagehelper.PageInfo;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Reference(version="1.0.0")
    private UserService userService;

    @PostMapping
    public boolean save(@RequestBody User user){
        return userService.save(user);
    }

    @DeleteMapping("{id}")
    public boolean delete(@PathVariable Integer id){
        return userService.delete(id);
    }

    @PutMapping
    public boolean update(@RequestBody User user){
        return userService.update(user);
    }

    @GetMapping("{page}/{size}")
    public PageInfo<User> findAll(@PathVariable("page") Integer page,
                                  @PathVariable("size") Integer size){
       return  userService.findAll(page,size);
    }

    @GetMapping("{id}")
    public User findById(@PathVariable Integer id){
        return userService.findById(id);
    }

}

