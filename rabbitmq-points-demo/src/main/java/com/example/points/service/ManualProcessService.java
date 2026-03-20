package com.example.points.service;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.points.entity.PointsLog;
import com.example.points.entity.PointsManualProcess;
import com.example.points.mapper.PointsManualProcessMapper;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class ManualProcessService {

    private final PointsManualProcessMapper manualProcessMapper;

    public void saveManualRecord(String bussinessId,Long userId,String error,Integer points) {
        PointsManualProcess exists = manualProcessMapper.selectOne(
                new QueryWrapper<PointsManualProcess>().eq("business_id",bussinessId)
        );
        if (exists != null) {
            return;
        }

        PointsManualProcess record = new PointsManualProcess();
        record.setUserId(userId);
        record.setBusinessId(bussinessId);
        record.setPoints(points);
        record.setStatus(0);
        record.setLastErrMsg(error);
        manualProcessMapper.insert(record);
    }
}
