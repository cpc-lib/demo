package com.example.points.service;

import com.example.points.domain.PointRewardCommand;
import com.example.points.domain.PointRewardResult;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class PointRewardApplicationService {
    private final PointRewardTransactionalService tx;

    public PointRewardApplicationService(PointRewardTransactionalService tx) {
        this.tx = tx;
    }

    public PointRewardResult reward(PointRewardCommand c) {
        try {
            tx.reward(c);
            return PointRewardResult.SUCCESS;
        } catch (DuplicateKeyException e) {
            return PointRewardResult.DUPLICATE;
        }
    }
}
