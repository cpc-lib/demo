package cc.ivera.user.service.impl;


import cc.ivera.user.domain.User;
import cc.ivera.user.mapper.UserMapper;
import cc.ivera.user.service.UserService;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

//暴露dubbo服务，
@DubboService(version = "2.0.0")
public class UserServiceImpl2 implements UserService {

    @Autowired
    private UserMapper userMapper;

    public String queryUsername(Long id) {
        return userMapper.findById(id).getUsername();
    }

    @Override
    public User queryById(Long id) {
        User user = userMapper.findById(id);
        user.setUsername(user.getUsername()+"v2.0");
        return user;
    }
}