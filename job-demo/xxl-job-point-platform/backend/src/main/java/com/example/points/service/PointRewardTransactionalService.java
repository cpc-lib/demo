package com.example.points.service;

import com.example.points.domain.PointRewardCommand;
import com.example.points.repository.PointRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class PointRewardTransactionalService {
    private final PointRepository repo;

    public PointRewardTransactionalService(PointRepository repo) {
        this.repo = repo;
    }

    @Transactional(rollbackFor = Exception.class)
    public void reward(PointRewardCommand c) {
        repo.insertLedger(c);
        int n = repo.addPoints(c.userId(), c.points());
        if (n != 1) throw new IllegalStateException("积分账户不存在或不可用, userId=" + c.userId());
    }
}
