package com.example.points.repository;

import com.example.points.domain.PointRewardCommand;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.sql.ResultSet;

@Repository
public class FailureRepository {
    private final JdbcTemplate jdbc;

    public FailureRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String left(String s, int n) {
        if (s == null) return null;
        return s.length() <= n ? s : s.substring(0, n);
    }

    public void upsert(PointRewardCommand c, String error) {
        jdbc.update("INSERT INTO point_reward_failure(batch_id,user_id,biz_no,biz_type,points,reward_date,status,attempt_count,last_error,created_at,updated_at) VALUES(?,?,?,?,?,?,'OPEN',1,?,NOW(),NOW()) ON DUPLICATE KEY UPDATE status='OPEN',attempt_count=attempt_count+1,last_error=?,updated_at=NOW()", c.batchId(), c.userId(), c.bizNo(), c.bizType(), c.points(), c.rewardDate(), left(error, 2000), left(error, 2000));
    }

    public void resolved(String bizNo) {
        jdbc.update("UPDATE point_reward_failure SET status='RESOLVED',updated_at=NOW() WHERE biz_no=? AND status<>'RESOLVED'", bizNo);
    }

    public PointRewardCommand findCommand(long id) {
        return jdbc.queryForObject("SELECT batch_id,user_id,biz_no,biz_type,points,reward_date FROM point_reward_failure WHERE id=?", (ResultSet rs, int row) -> new PointRewardCommand(rs.getLong("batch_id"), rs.getLong("user_id"), rs.getString("biz_no"), rs.getString("biz_type"), rs.getLong("points"), rs.getDate("reward_date").toLocalDate()), id);
    }

    public void markRetrying(long id) {
        jdbc.update("UPDATE point_reward_failure SET status='RETRYING',updated_at=NOW() WHERE id=?", id);
    }
}
