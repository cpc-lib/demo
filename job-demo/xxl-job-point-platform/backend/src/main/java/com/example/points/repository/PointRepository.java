package com.example.points.repository;

import com.example.points.domain.PointRewardCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class PointRepository {
    private final JdbcTemplate jdbc;

    public PointRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public void insertLedger(PointRewardCommand c) {
        jdbc.update("INSERT INTO user_point_ledger(user_id,batch_id,biz_type,biz_no,change_point,reward_date) VALUES(?,?,?,?,?,?)", c.userId(), c.batchId(), c.bizType(), c.bizNo(), c.points(), c.rewardDate());
    }

    public int addPoints(Long userId, Long points) {
        return jdbc.update("UPDATE user_account SET points=points+?,version=version+1,updated_at=NOW() WHERE id=? AND status='ACTIVE'", points, userId);
    }
}
