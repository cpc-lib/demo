package com.example.points.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;

@Repository
public class BatchRepository {
    private final JdbcTemplate jdbc;

    public BatchRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    private static String left(String s, int n) {
        if (s == null) return null;
        return s.length() <= n ? s : s.substring(0, n);
    }

    public long getOrCreate(LocalDate date, int shardTotal) {
        jdbc.update("INSERT INTO point_reward_batch(biz_type,reward_date,status,shard_total,started_at) VALUES('DAILY_REWARD',?,'RUNNING',?,NOW()) ON DUPLICATE KEY UPDATE shard_total=GREATEST(shard_total,?),status=IF(status='SUCCESS','SUCCESS','RUNNING')", date, shardTotal, shardTotal);
        return jdbc.queryForObject("SELECT id FROM point_reward_batch WHERE biz_type='DAILY_REWARD' AND reward_date=?", Long.class, date);
    }

    public void markShardRunning(long batchId, int shard) {
        jdbc.update("INSERT INTO point_reward_batch_shard(batch_id,shard_index,status,started_at) VALUES(?,?,'RUNNING',NOW()) ON DUPLICATE KEY UPDATE status='RUNNING',started_at=NOW(),finished_at=NULL,last_error=NULL", batchId, shard);
    }

    public void markShardSuccess(long batchId, int shard) {
        jdbc.update("UPDATE point_reward_batch_shard SET status='SUCCESS',finished_at=NOW() WHERE batch_id=? AND shard_index=?", batchId, shard);
    }

    public void markShardFailed(long batchId, int shard, String error) {
        jdbc.update("UPDATE point_reward_batch_shard SET status='FAILED',finished_at=NOW(),last_error=? WHERE batch_id=? AND shard_index=?", left(error, 1000), batchId, shard);
        jdbc.update("UPDATE point_reward_batch SET status='FAILED',remark=? WHERE id=?", left(error, 1000), batchId);
    }

    public void increment(long batchId, long scanned, long sent) {
        jdbc.update("UPDATE point_reward_batch SET scanned_count=scanned_count+?,sent_count=sent_count+? WHERE id=?", scanned, sent, batchId);
    }

    public void tryComplete(long batchId) {
        Integer total = jdbc.queryForObject("SELECT shard_total FROM point_reward_batch WHERE id=?", Integer.class, batchId);
        Long success = jdbc.queryForObject("SELECT COUNT(*) FROM point_reward_batch_shard WHERE batch_id=? AND status='SUCCESS'", Long.class, batchId);
        if (total != null && success != null && success >= total)
            jdbc.update("UPDATE point_reward_batch SET status='SUCCESS',finished_at=NOW(),remark=NULL WHERE id=?", batchId);
    }
}
