package com.example.points.service;

import com.example.points.entity.DeadMessage;
import com.example.points.mapper.DeadMessageMapper;
import com.example.points.mq.RegisterProducer;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManualProcessService {

    private final DeadMessageMapper deadMessageMapper;
    private final RegisterProducer registerProducer;

    public void reSend(Long deadId) {
        DeadMessage dm = deadMessageMapper.selectById(deadId);
        if (dm == null) {
            return;
        }
        // 简化：只重新发送 userId
        registerProducer.sendRegisterMsg(dm.getUserId());
        // 这里可以选择删除或保留记录
        deadMessageMapper.deleteById(deadId);
    }
}
