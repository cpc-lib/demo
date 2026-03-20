package cc.ivera.controller;

import com.github.pagehelper.PageHelper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;
import cc.ivera.domain.User;
import cc.ivera.service.UserService;

import java.util.List;

@RequestMapping("/user")
public class UserController {

    @Autowired
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
    public List<User> findAll(@PathVariable Integer page,
                                  @PathVariable Integer size){
        //分页拦截，此处执行语句count与limit控制语句(MyBatis Log的使用)
        PageHelper.startPage(page,size);
        List<User> userList = userService.findAll();
        return userList;
    }

    @GetMapping("{id}")
    public User findById(@PathVariable Long id){
        return userService.findById(id);
    }

}
