package cc.ivera.service.impl;

import cc.ivera.mapper.UserMapper;
import cc.ivera.pojo.User;
import cc.ivera.service.UserService;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserServiceImpl implements UserService {

    @Autowired
    private UserMapper userMapper;

    @Override
    public boolean save(User user) {
        return userMapper.save(user)==1;
    }

    @Override
    public boolean delete(Integer id) {
        return userMapper.delete(id)==1;
    }

    @Override
    public boolean update(User user) {
        return userMapper.update(user)==1;
    }

    @Override
    public PageInfo<User> findAll(Integer page,Integer size) {
        PageHelper.startPage(page,size);
        List<User> all = userMapper.findAll();
        return  new PageInfo<User>(all);
    }

    @Override
    public User findById(Integer id) {
        return userMapper.findById(id);
    }
}
