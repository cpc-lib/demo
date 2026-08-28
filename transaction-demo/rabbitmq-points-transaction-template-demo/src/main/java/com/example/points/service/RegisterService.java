package com.example.points.service;

import com.example.points.entity.User;
import com.example.points.mapper.UserMapper;
import com.example.points.mq.RegisterProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class RegisterService {

    private final UserMapper userMapper;
    private final RegisterProducer registerProducer;

    @Transactional
    public Long register(String username, String mobile) {
        User u = new User();
        u.setUsername(username);
        u.setMobile(mobile);
        u.setCreateTime(new Date());
        userMapper.insert(u);

        // 注册完成后发送 MQ 消息（送积分）
        registerProducer.sendRegisterMsg(u.getId());

        return u.getId();
    }
}
