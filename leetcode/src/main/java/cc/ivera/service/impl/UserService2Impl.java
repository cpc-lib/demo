package cc.ivera.service.impl;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import cc.ivera.mapper.easyexcel.User2Mapper;
import cc.ivera.model.pojo.easyexcel.User2;
import cc.ivera.service.UserService2;

import java.util.List;

@Service
public class UserService2Impl extends ServiceImpl<User2Mapper, User2> implements UserService2 {

    @Autowired
    private User2Mapper user2Mapper;


    @Override
    public List<User2> getUserList() {
        QueryWrapper<User2> queryWrapper = new QueryWrapper<User2>().le("id", 1000);
        return user2Mapper.selectList(queryWrapper);
    }
}
