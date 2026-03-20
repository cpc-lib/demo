package cc.ivera.service.impl;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import cc.ivera.domain.User;
import cc.ivera.mapper.UserMapper;
import cc.ivera.service.UserService;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl extends ServiceImpl<UserMapper, User> implements UserService {

}
