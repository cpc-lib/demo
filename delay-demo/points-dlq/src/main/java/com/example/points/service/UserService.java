package com.example.points.service;

import com.example.points.config.RabbitConfig;
import com.example.points.dto.UserRegisterMsg;
import com.example.points.entity.UserInfo;
import com.example.points.mapper.UserInfoMapper;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class UserService {

    private final UserInfoMapper userInfoMapper;
    private final RabbitTemplate rabbitTemplate;
    private final ObjectMapper objectMapper;

    @Transactional
    public Long registerUser(String username, String mobile) throws JsonProcessingException {
        UserInfo user = new UserInfo();
        user.setUsername(username);
        user.setMobile(mobile);
        userInfoMapper.insert(user);

        String businessId = "reg_" + user.getId();
        UserRegisterMsg msg = new UserRegisterMsg(user.getId(), businessId);
        String content = objectMapper.writeValueAsString(msg);

        rabbitTemplate.convertAndSend(
                RabbitConfig.USER_REGISTER_EXCHANGE,
                RabbitConfig.USER_REGISTER_ROUTING_KEY,
                content
        );

        return user.getId();
    }
}
