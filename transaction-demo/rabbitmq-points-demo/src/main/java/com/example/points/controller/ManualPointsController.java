package com.example.points.controller;

import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.example.points.dto.UserRegisterMsg;
import com.example.points.entity.PointsManualProcess;
import com.example.points.mapper.PointsManualProcessMapper;
import com.example.points.service.PointsService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/manualPoints")
@RequiredArgsConstructor
public class ManualPointsController {

    private final PointsService pointsService;
    private final PointsManualProcessMapper manualProcessMapper;

    /**
     * 查询待人工处理列表
     */
    @GetMapping("/pending")
    public List<PointsManualProcess> pendingList() {
        return manualProcessMapper.selectList(
                new QueryWrapper<PointsManualProcess>().eq("status", 0)
        );
    }

    /**
     * 人工点击处理
     */
    @PostMapping("/process/{businessId}")
    public String manualProcess(@PathVariable String businessId) {
        PointsManualProcess record = manualProcessMapper.selectOne(
                new QueryWrapper<PointsManualProcess>().eq("business_id", businessId)
        );
        if (record == null) {
            return "记录不存在";
        }
        if (record.getStatus() == 1) {
            return "已处理";
        }

        pointsService.processRegisterEvent(
                new UserRegisterMsg(record.getUserId(), record.getBusinessId())
        );

        record.setStatus(1);
        manualProcessMapper.updateById(record);
        return "处理成功";
    }
}
