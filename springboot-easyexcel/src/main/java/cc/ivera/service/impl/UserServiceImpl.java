package cc.ivera.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import cc.ivera.domain.User;
import cc.ivera.mapper.UserMapper;
import cc.ivera.service.UserService;

import javax.annotation.Resource;
import java.util.List;

@Service("userService")
public class UserServiceImpl extends ServiceImpl implements UserService {

    @Resource
    private UserMapper userMapper;


    @Override
    public List<User> getUserList() {
        QueryWrapper<User> queryWrapper = new QueryWrapper<User>().le("id", 1000);
        return userMapper.selectList(queryWrapper);
    }
}
