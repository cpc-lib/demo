package cc.ivera.migration.task;

import cc.ivera.migration.config.MigrationProperties;
import cc.ivera.migration.domain.MigrationStatus;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicLong;

@RequiredArgsConstructor
public class ShardMigrationWorker implements Runnable {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final MigrationProperties properties;
    private final String taskNo;
    private final String sourceTable;
    private final String tmpTable;
    private final String idColumn;
    private final int batchSize;
    private final Map<String, Object> shard;
    private final AtomicLong globalSuccess;
    private static final ConcurrentHashMap<Integer, Object> shardLocks = new ConcurrentHashMap<>();

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
                logWithShard("ERROR", "SHARD", "分片失败 shardNo=" + shardNo + ", retry=" + retry + ", error=" + msg, shardNo);
                if (retry >= properties.getMaxRetryTimes()) return;
                try { Thread.sleep(Math.min(1000L * retry, 3000L)); } catch (InterruptedException ignored) { Thread.currentThread().interrupt(); return; }
            }
        }
    }

    private void doRun(int shardNo, long startId, long endId, int retry) {
        jdbcTemplate.update("UPDATE migration_plan_shard SET status=?, start_time=IFNULL(start_time,NOW()), error_message=NULL, retry_count=? WHERE task_no=? AND shard_no=?",
                MigrationStatus.RUNNING, retry, taskNo, shardNo);
        logWithShard("INFO", "SHARD", "开始分片 shardNo=" + shardNo + ", keysetRange=" + startId + "~" + endId + ", idColumn=" + idColumn, shardNo);

        long success = 0;
        int batchNo = 1;
        Long lastCursor = null;
        Object shardLock = shardLocks.computeIfAbsent(shardNo, k -> new Object());

        while (true) {
            long begin = System.currentTimeMillis();
            List<Long> ids = nextExistingIds(startId, endId, lastCursor);
            if (ids.isEmpty()) {
                break;
            }
            long batchStart = ids.get(0);
            long batchEnd = ids.get(ids.size() - 1);
            int selectedRows = ids.size();

            try {
                migrateBatch(batchStart, batchEnd);
                long cost = Math.max(System.currentTimeMillis() - begin, 1);
                double tps = selectedRows * 1000.0 / cost;
                success += selectedRows;
                globalSuccess.addAndGet(selectedRows);
                synchronized (shardLock) {
                    batchNo++;
                }
                insertBatchLog(shardNo, batchNo - 1, batchStart, batchEnd, selectedRows, cost, tps, MigrationStatus.SUCCESS, null);
                lastCursor = batchEnd;
            } catch (Exception e) {
                long cost = Math.max(System.currentTimeMillis() - begin, 1);
                synchronized (shardLock) {
                    batchNo++;
                }
                insertBatchLog(shardNo, batchNo - 1, batchStart, batchEnd, 0, cost, 0, MigrationStatus.FAILED, rootMsg(e));
                throw e;
            }
        }

        jdbcTemplate.update("UPDATE migration_plan_shard SET status=?, success_count=?, fail_count=0, end_time=NOW(), error_message=NULL WHERE task_no=? AND shard_no=?",
                MigrationStatus.SUCCESS, success, taskNo, shardNo);
        logWithShard("INFO", "SHARD", "完成分片 shardNo=" + shardNo + ", success=" + success, shardNo);
        shardLocks.remove(shardNo);
    }

    /**
     * 执行阶段也不能 batchStart += batchSize。
     * 这里使用 keyset 游标：id > lastCursor ORDER BY id LIMIT batchSize。
     * 因此即使 id 有巨大断层，也只扫描真实存在的行。
     */
    private List<Long> nextExistingIds(long startId, long endId, Long lastCursor) {
        if (lastCursor == null) {
            return jdbcTemplate.queryForList(
                    "SELECT " + idColumn + " FROM " + sourceTable + " WHERE " + idColumn + " >= ? AND " + idColumn + " <= ? ORDER BY " + idColumn + " LIMIT ?",
                    Long.class, startId, endId, batchSize
            );
        }
        return jdbcTemplate.queryForList(
                "SELECT " + idColumn + " FROM " + sourceTable + " WHERE " + idColumn + " > ? AND " + idColumn + " <= ? ORDER BY " + idColumn + " LIMIT ?",
                Long.class, lastCursor, endId, batchSize
        );
    }

    /**
     * 同库迁移最快方式：INSERT INTO tmp SELECT FROM source。
     * 为了支持失败重跑幂等，使用 ON DUPLICATE KEY UPDATE，重复执行不会产生重复数据。
     * 注意：MySQL 对 ON DUPLICATE KEY UPDATE 的 affected rows 可能按 2 计算更新行，
     * 所以外层以 selectedRows 作为本批成功数，避免重试时统计翻倍。
     */
    private void migrateBatch(long startId, long endId) {
        String sql = "INSERT INTO " + tmpTable + "(id,user_name,age,phone,email,create_time,update_time) " +
                "SELECT id,user_name,age,phone,email,create_time,update_time FROM " + sourceTable + " WHERE " + idColumn + " BETWEEN ? AND ? " +
                "ON DUPLICATE KEY UPDATE user_name=VALUES(user_name), age=VALUES(age), phone=VALUES(phone), email=VALUES(email), update_time=VALUES(update_time)";
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.update(sql, startId, endId));
    }

    private void insertBatchLog(int shardNo, int batchNo, long startId, long endId, int affected, long costMs, double tps, String status, String err) {
        jdbcTemplate.update("INSERT INTO migration_batch_log(task_no, shard_no, batch_no, start_id, end_id, affected_rows, cost_ms, tps, status, error_message) " +
                        "VALUES(?,?,?,?,?,?,?,?,?,?) ON DUPLICATE KEY UPDATE affected_rows=VALUES(affected_rows), cost_ms=VALUES(cost_ms), tps=VALUES(tps), status=VALUES(status), error_message=VALUES(error_message)",
                taskNo, shardNo, batchNo, startId, endId, affected, costMs, tps, status, err);
    }
    
    private void logWithShard(String level, String stage, String msg, int shardNo) {
        jdbcTemplate.update("INSERT INTO migration_log(task_no, level, stage, message, shard_no) VALUES(?,?,?,?,?)", 
                taskNo, level, stage, msg, shardNo);
    }
    private String rootMsg(Throwable e) { Throwable t=e; while(t.getCause()!=null) t=t.getCause(); return t.getMessage(); }
}
