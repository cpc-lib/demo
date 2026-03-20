package cc.ivera.service.impl;

import cc.ivera.dao.UserDao;
import cc.ivera.domain.User;
import cc.ivera.service.UserService;
import com.alibaba.dubbo.config.annotation.Service;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.github.pagehelper.PageHelper;
import com.github.pagehelper.PageInfo;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;

@Service(version="1.0.0",interfaceClass= UserService.class )
public class UserServiceImpl implements UserService {

    @Autowired
    private UserDao userDao;

    @Override
    public boolean save(User user) {
        return userDao.insert(user)==1;
    }

    @Override
    public boolean delete(Integer id) {
        return userDao.deleteById(id)==1;
    }

    @Override
    public boolean update(User user) {
        return userDao.updateById(user)==1;
    }

    @Override
    public PageInfo<User> findAll(Integer page, Integer size) {
        PageHelper.startPage(page, size);
        QueryWrapper<User> wrapper=new QueryWrapper<>();
        wrapper.select("id","username","password");
        List<User> all = userDao.selectList(wrapper);
        PageInfo<User> pageInfo = new PageInfo<User>(all);
        return pageInfo;
    }

    @Override
    public User findById(Integer id) {
        return userDao.selectById(id);
    }
}
