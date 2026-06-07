package cc.ivera.migration.task;

import cc.ivera.migration.config.MigrationProperties;
import cc.ivera.migration.domain.MigrationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.dao.DataAccessException;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.Map;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class ShardMigrationWorker implements Runnable {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final MigrationProperties properties;
    private final String taskNo;
    private final String sourceTable;
    private final String tmpTable;
    private final int batchSize;
    private final Map<String, Object> shard;
    private final AtomicLong globalSuccess;

    @Override
    public void run() {
        int shardNo = ((Number) shard.get("shard_no")).intValue();
        long startId = ((Number) shard.get("start_id")).longValue();
        long endId = ((Number) shard.get("end_id")).longValue();
        int retry = 0;
        while (retry < properties.getMaxRetryTimes()) {
            try {
                doRun(shardNo, startId, endId, retry);
                return;
            } catch (Exception e) {
                retry++;
                String msg = rootMsg(e);
                jdbcTemplate.update("UPDATE migration_plan_shard SET retry_count=?, status=?, error_message=? WHERE task_no=? AND shard_no=?",
                        retry, retry >= properties.getMaxRetryTimes() ? MigrationStatus.FAILED : MigrationStatus.RETRYING, msg, taskNo, shardNo);
                log("ERROR", "SHARD", "分片失败 shardNo=" + shardNo + ", retry=" + retry + ", error=" + msg);
                if (retry >= properties.getMaxRetryTimes()) return;
                try { Thread.sleep(Math.min(1000L * retry, 3000L)); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private void doRun(int shardNo, long startId, long endId, int retry) {
        jdbcTemplate.update("UPDATE migration_plan_shard SET status=?, start_time=IFNULL(start_time,NOW()), error_message=NULL, retry_count=? WHERE task_no=? AND shard_no=?",
                MigrationStatus.RUNNING, retry, taskNo, shardNo);
        log("INFO", "SHARD", "开始分片 shardNo=" + shardNo + ", range=" + startId + "~" + endId);

        long success = 0;
        int batchNo = 1;
        for (long batchStart = startId; batchStart <= endId; batchStart += batchSize) {
            long batchEnd = Math.min(batchStart + batchSize - 1, endId);
            long begin = System.currentTimeMillis();
            try {
                int affected = migrateBatch(batchStart, batchEnd);
                long cost = Math.max(System.currentTimeMillis() - begin, 1);
                double tps = affected * 1000.0 / cost;
                success += affected;
                globalSuccess.addAndGet(affected);
                insertBatchLog(shardNo, batchNo, batchStart, batchEnd, affected, cost, tps, MigrationStatus.SUCCESS, null);
            } catch (Exception e) {
                long cost = Math.max(System.currentTimeMillis() - begin, 1);
                insertBatchLog(shardNo, batchNo, batchStart, batchEnd, 0, cost, 0, MigrationStatus.FAILED, rootMsg(e));
                throw e;
            }
            batchNo++;
        }
        jdbcTemplate.update("UPDATE migration_plan_shard SET status=?, success_count=?, fail_count=0, end_time=NOW(), error_message=NULL WHERE task_no=? AND shard_no=?",
                MigrationStatus.SUCCESS, success, taskNo, shardNo);
        log("INFO", "SHARD", "完成分片 shardNo=" + shardNo + ", success=" + success);
    }

    /**
     * 同库迁移最快方式：INSERT INTO tmp SELECT FROM source。
     * 为了支持失败重跑幂等，使用 ON DUPLICATE KEY UPDATE，重复执行不会产生重复数据。
     */
    private int migrateBatch(long startId, long endId) throws DataAccessException {
        String sql = "INSERT INTO " + tmpTable + "(id,user_name,age,phone,email,create_time,update_time) " +
                "SELECT id,user_name,age,phone,email,create_time,update_time FROM " + sourceTable + " WHERE id BETWEEN ? AND ? " +
                "ON DUPLICATE KEY UPDATE user_name=VALUES(user_name), age=VALUES(age), phone=VALUES(phone), email=VALUES(email), update_time=VALUES(update_time)";
        return transactionTemplate.execute(status -> jdbcTemplate.update(sql, startId, endId));
    }

    private void insertBatchLog(int shardNo, int batchNo, long startId, long endId, int affected, long costMs, double tps, String status, String err) {
        jdbcTemplate.update("INSERT INTO migration_batch_log(task_no, shard_no, batch_no, start_id, end_id, affected_rows, cost_ms, tps, status, error_message) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE affected_rows=VALUES(affected_rows), cost_ms=VALUES(cost_ms), tps=VALUES(tps), status=VALUES(status), error_message=VALUES(error_message)",
                taskNo, shardNo, batchNo, startId, endId, affected, costMs, tps, status, err);
    }
    private void log(String level, String stage, String msg) { jdbcTemplate.update("INSERT INTO migration_log(task_no, level, stage, message) VALUES(?,?,?,?)", taskNo, level, stage, msg); }
    private String rootMsg(Throwable e) { Throwable t=e; while(t.getCause()!=null) t=t.getCause(); return t.getMessage(); }
}
