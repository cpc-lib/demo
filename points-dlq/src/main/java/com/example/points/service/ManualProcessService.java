package com.example.points.service;

import com.example.points.entity.ManualProcess;
import com.example.points.mapper.ManualProcessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class ManualProcessService {

    private final ManualProcessMapper manualProcessMapper;

    public void saveManualRecord(String businessId, Long userId, String errMsg) {
        ManualProcess mp = new ManualProcess();
        mp.setBusinessId(businessId);
        mp.setUserId(userId);
        mp.setErrMsg(errMsg);
        mp.setStatus(0);
        mp.setCreateTime(new Date());
        manualProcessMapper.insert(mp);
    }
}
