package cc.ivera.service;


import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.stereotype.Service;
import cc.ivera.mapper.UserMapper;
import cc.ivera.model.pojo.User;

@Service
public class UserService extends ServiceImpl<UserMapper, User> {

}
