package cc.ivera.service;

import cc.ivera.entity.User;
import cc.ivera.mapper.UserMapper;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class UserService {

    @Autowired
    private UserMapper userMapper;

    public List<User> findUserList() {
        return userMapper.selectUserList();
    }

    public User findOneUserById(Long id) {
        return userMapper.selectOneUser(id);
    }

    public User findOneUserByName(String userName) {
        return userMapper.selectOneUserByName(userName);
    }
}
