package cc.ivera.user.service.impl;


import cc.ivera.dubbo.api.UserService;
import cc.ivera.dubbo.domain.User;
import cc.ivera.user.mapper.UserMapper;
import org.apache.dubbo.config.annotation.DubboService;
import org.springframework.beans.factory.annotation.Autowired;

@DubboService
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    public User queryById(Long id) {
        return userMapper.findById(id);
    }
}