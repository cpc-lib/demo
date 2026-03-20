package cc.ivera.controller;

import cc.ivera.pojo.User;
import cc.ivera.service.UserService;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/user")
public class UserController {

    @Autowired
    private UserService userService;

    @GetMapping("{page}/{size}")
    public PageInfo<User> findAll(@PathVariable Integer page,
                                  @PathVariable Integer size){
        return userService.findAll(page,size);
    }

    @PostMapping
    public boolean save(@RequestBody User user){
        return userService.save(user);
    }

    @PutMapping
    public boolean update(@RequestBody User user){
        return userService.update(user);
    }

    @GetMapping("{id}")
    public User findById(@PathVariable Integer id){
        return userService.findById(id);
    }

    @DeleteMapping("{id}")
    public boolean delete(@PathVariable Integer id){
        return userService.delete(id);
    }
}
