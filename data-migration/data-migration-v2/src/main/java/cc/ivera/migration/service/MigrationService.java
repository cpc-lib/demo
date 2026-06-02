package cc.ivera.migration.service;

import cc.ivera.migration.config.MigrationProperties;
import cc.ivera.migration.domain.MigrationStatus;
import cc.ivera.migration.domain.PlanRequest;
import cc.ivera.migration.task.ShardMigrationWorker;
import cc.ivera.migration.util.SqlGuard;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.support.TransactionTemplate;

import java.math.BigDecimal;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicLong;

@Service
@RequiredArgsConstructor
public class MigrationService {
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final MigrationProperties properties;

    /**
     * 生产环境不能假设 id 连续。
     *
     * 旧版本问题：
     * 1. SELECT MIN(id), MAX(id) 后用 shardSize 做数值切片；
     * 2. 执行阶段 batchStart += batchSize。
     * 如果 id 是雪花 ID、删除后断层、历史归档断层，会生成大量空区间。
     *
     * 当前版本：
     * 只扫描真实存在的主键，按“行数”切片：每 shardSize 个真实 id 形成一个分片。
     * 分片仍保存 start_id/end_id，执行时再用 keyset 游标推进。
     */
    public Map<String, Object> plan(PlanRequest req) {
        String source = SqlGuard.safeName(req.getSourceTable(), properties.getTableNameRegex());
        String target = SqlGuard.safeName(req.getTargetTable(), properties.getTableNameRegex());
        String idColumn = SqlGuard.safeName(req.getIdColumn(), properties.getTableNameRegex());
        int shardSize = positive(req.getShardSize(), properties.getDefaultShardSize());
        int batchSize = positive(req.getBatchSize(), properties.getDefaultBatchSize());
        int threadSize = Math.min(positive(req.getThreadSize(), properties.getDefaultThreadSize()), properties.getMaxThreadSize());
        String taskNo = "MIG" + LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMddHHmmssSSS"));
        String tmpTable = target + "_tmp_" + taskNo.toLowerCase(Locale.ROOT);

        List<ShardRange> shardRanges = buildKeysetShards(source, idColumn, shardSize);
        if (shardRanges.isEmpty()) {
            throw new IllegalStateException("源表没有数据");
        }
        long minId = shardRanges.get(0).startId();
        long maxId = shardRanges.get(shardRanges.size() - 1).endId();
        long total = shardRanges.stream().mapToLong(ShardRange::plannedCount).sum();

        transactionTemplate.executeWithoutResult(s -> {
            jdbcTemplate.update("CREATE TABLE IF NOT EXISTS " + target + " LIKE " + source);
            jdbcTemplate.update("DROP TABLE IF EXISTS " + tmpTable);
            jdbcTemplate.update("CREATE TABLE " + tmpTable + " LIKE " + source);
            // 临时表保留主键，用 ON DUPLICATE KEY UPDATE 实现重试幂等
            jdbcTemplate.update("INSERT INTO migration_task(task_no, source_table, target_table, tmp_table, id_column, min_id, max_id, total_count, shard_size, batch_size, thread_size, status) VALUES(?,?,?,?,?,?,?,?,?,?,?,?)",
                    taskNo, source, target, tmpTable, idColumn, minId, maxId, total, shardSize, batchSize, threadSize, MigrationStatus.PLANNED);
            int shardNo = 1;
            for (ShardRange range : shardRanges) {
                jdbcTemplate.update("INSERT INTO migration_plan_shard(task_no, shard_no, start_id, end_id, planned_count, status) VALUES(?,?,?,?,?,?)",
                        taskNo, shardNo++, range.startId(), range.endId(), range.plannedCount(), MigrationStatus.PLANNED);
            }
            log(taskNo, "INFO", "PLAN", "Keyset规划完成：source=" + source + ", target=" + target + ", tmp=" + tmpTable +
                    ", idColumn=" + idColumn + ", total=" + total + ", shardCount=" + shardRanges.size() +
                    ", shardSize=" + shardSize + ", batchSize=" + batchSize + ", threadSize=" + threadSize +
                    ", minId=" + minId + ", maxId=" + maxId);
        });
        return task(taskNo);
    }

    /**
     * 只按真实存在的主键生成分片。
     * MySQL Connector/J 开启 useCursorFetch=false 时，Integer.MIN_VALUE 可启用流式读取，避免一次性拉取全部 id 到内存。
     * 这里内存中只保留分片边界，不保留百万/千万个 id。
     */
    private List<ShardRange> buildKeysetShards(String source, String idColumn, int shardSize) {
        List<ShardRange> ranges = new ArrayList<>();
        final long[] startId = new long[1];
        final long[] endId = new long[1];
        final int[] count = new int[1];
        final boolean[] opened = new boolean[1];

        jdbcTemplate.query(connection -> {
            PreparedStatement ps = connection.prepareStatement(
                    "SELECT " + idColumn + " FROM " + source + " ORDER BY " + idColumn,
                    ResultSet.TYPE_FORWARD_ONLY,
                    ResultSet.CONCUR_READ_ONLY
            );
            ps.setFetchSize(Integer.MIN_VALUE);
            return ps;
        }, rs -> {
            long id = rs.getLong(1);
            if (!opened[0]) {
                startId[0] = id;
                opened[0] = true;
            }
            endId[0] = id;
            count[0]++;
            if (count[0] >= shardSize) {
                ranges.add(new ShardRange(startId[0], endId[0], count[0]));
                opened[0] = false;
                count[0] = 0;
            }
        });

        if (opened[0] && count[0] > 0) {
            ranges.add(new ShardRange(startId[0], endId[0], count[0]));
        }
        return ranges;
    }

    public Map<String, Object> execute(String taskNo) {
        Map<String, Object> task = task(taskNo);
        String status = String.valueOf(task.get("status"));
        if (!(MigrationStatus.PLANNED.equals(status) || MigrationStatus.PARTIAL_FAILED.equals(status) || MigrationStatus.FAILED.equals(status))) {
            throw new IllegalStateException("当前状态不允许执行: " + status);
        }
        return runShards(taskNo, false);
    }

    public Map<String, Object> retryFailed(String taskNo) {
        return runShards(taskNo, true);
    }

    private Map<String, Object> runShards(String taskNo, boolean onlyFailed) {
        Map<String, Object> task = task(taskNo);
        String source = String.valueOf(task.get("source_table"));
        String tmp = String.valueOf(task.get("tmp_table"));
        String idColumn = String.valueOf(task.get("id_column"));
        int batchSize = ((Number) task.get("batch_size")).intValue();
        int threadSize = ((Number) task.get("thread_size")).intValue();
        List<Map<String, Object>> shardList = onlyFailed
                ? jdbcTemplate.queryForList("SELECT * FROM migration_plan_shard WHERE task_no=? AND status=? ORDER BY shard_no", taskNo, MigrationStatus.FAILED)
                : jdbcTemplate.queryForList("SELECT * FROM migration_plan_shard WHERE task_no=? AND status IN (?,?) ORDER BY shard_no", taskNo, MigrationStatus.PLANNED, MigrationStatus.FAILED);
        if (shardList.isEmpty()) {
            updateTaskProgress(taskNo);
            return task(taskNo);
        }

        jdbcTemplate.update("UPDATE migration_task SET status=?, start_time=IFNULL(start_time,NOW()), error_message=NULL WHERE task_no=?", onlyFailed ? MigrationStatus.RETRYING : MigrationStatus.RUNNING, taskNo);
        log(taskNo, "INFO", onlyFailed ? "RETRY" : "EXECUTE", "开始执行分片数=" + shardList.size() + ", threadSize=" + threadSize + ", idColumn=" + idColumn);
        ExecutorService pool = new ThreadPoolExecutor(threadSize, threadSize, 60, TimeUnit.SECONDS, new LinkedBlockingQueue<>(threadSize * 4), new ThreadPoolExecutor.CallerRunsPolicy());
        List<Future<?>> futures = new ArrayList<>();
        long begin = System.currentTimeMillis();
        AtomicLong success = new AtomicLong();

        for (Map<String, Object> shard : shardList) {
            futures.add(pool.submit(new ShardMigrationWorker(jdbcTemplate, transactionTemplate, properties, taskNo, source, tmp, idColumn, batchSize, shard, success)));
        }
        pool.shutdown();
        List<String> errors = new ArrayList<>();
        for (Future<?> f : futures) {
            try { f.get(); } catch (Exception e) { errors.add(rootMsg(e)); }
        }
        long cost = Math.max(System.currentTimeMillis() - begin, 1);
        updateTaskProgress(taskNo);
        long failed = Optional.ofNullable(jdbcTemplate.queryForObject("SELECT COUNT(*) FROM migration_plan_shard WHERE task_no=? AND status=?", Long.class, taskNo, MigrationStatus.FAILED)).orElse(0L);
        long ok = Optional.ofNullable(jdbcTemplate.queryForObject("SELECT COALESCE(SUM(success_count),0) FROM migration_plan_shard WHERE task_no=?", Long.class, taskNo)).orElse(0L);
        BigDecimal tps = BigDecimal.valueOf(ok * 1000.0 / cost);
        String newStatus = failed == 0 ? MigrationStatus.SUCCESS : MigrationStatus.PARTIAL_FAILED;
        jdbcTemplate.update("UPDATE migration_task SET status=?, success_count=?, fail_count=?, tps=?, end_time=NOW(), error_message=? WHERE task_no=?",
                newStatus, ok, failed, tps, errors.isEmpty() ? null : String.join(";", errors), taskNo);
        log(taskNo, failed == 0 ? "INFO" : "ERROR", "EXECUTE", "执行结束：status=" + newStatus + ", success=" + ok + ", failedShards=" + failed + ", tps=" + tps);
        return task(taskNo);
    }

    public Map<String, Object> verify(String taskNo) {
        Map<String, Object> task = task(taskNo);
        String source = String.valueOf(task.get("source_table"));
        String tmp = String.valueOf(task.get("tmp_table"));
        String idColumn = String.valueOf(task.get("id_column"));
        jdbcTemplate.update("UPDATE migration_task SET status=? WHERE task_no=?", MigrationStatus.VERIFYING, taskNo);

        Long sourceCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + source + " WHERE " + idColumn + " BETWEEN ? AND ?", Long.class, task.get("min_id"), task.get("max_id"));
        Long tmpCount = jdbcTemplate.queryForObject("SELECT COUNT(*) FROM " + tmp, Long.class);
        BigDecimal sourceChecksum = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(CAST(" + idColumn + " AS DECIMAL(38,0))),0) FROM " + source + " WHERE " + idColumn + " BETWEEN ? AND ?", BigDecimal.class, task.get("min_id"), task.get("max_id"));
        BigDecimal tmpChecksum = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(CAST(" + idColumn + " AS DECIMAL(38,0))),0) FROM " + tmp, BigDecimal.class);
        boolean pass = Objects.equals(sourceCount, tmpCount) && Objects.equals(sourceChecksum, tmpChecksum);
        jdbcTemplate.update("UPDATE migration_task SET status=?, source_checksum=?, tmp_checksum=?, error_message=? WHERE task_no=?",
                pass ? MigrationStatus.VERIFIED : MigrationStatus.FAILED, sourceChecksum, tmpChecksum, pass ? null : "校验失败：sourceCount=" + sourceCount + ", tmpCount=" + tmpCount, taskNo);
        log(taskNo, pass ? "INFO" : "ERROR", "VERIFY", "sourceCount=" + sourceCount + ", tmpCount=" + tmpCount + ", sourceChecksum=" + sourceChecksum + ", tmpChecksum=" + tmpChecksum);
        return Map.of("taskNo", taskNo, "pass", pass, "sourceCount", sourceCount, "tmpCount", tmpCount, "sourceChecksum", sourceChecksum, "tmpChecksum", tmpChecksum);
    }

    public Map<String, Object> switchTable(String taskNo) {
        Map<String, Object> task = task(taskNo);
        if (!MigrationStatus.VERIFIED.equals(String.valueOf(task.get("status")))) {
            throw new IllegalStateException("必须先校验通过再切换，当前状态=" + task.get("status"));
        }
        String target = String.valueOf(task.get("target_table"));
        String tmp = String.valueOf(task.get("tmp_table"));
        String idColumn = String.valueOf(task.get("id_column"));
        transactionTemplate.executeWithoutResult(s -> {
            jdbcTemplate.update("TRUNCATE TABLE " + target);
            jdbcTemplate.update("INSERT INTO " + target + " SELECT * FROM " + tmp);
            BigDecimal targetChecksum = jdbcTemplate.queryForObject("SELECT COALESCE(SUM(CAST(" + idColumn + " AS DECIMAL(38,0))),0) FROM " + target, BigDecimal.class);
            jdbcTemplate.update("UPDATE migration_task SET status=?, target_checksum=? WHERE task_no=?", MigrationStatus.SWITCHED, targetChecksum, taskNo);
            log(taskNo, "INFO", "SWITCH", "已从临时表切换到目标表：" + tmp + " -> " + target + ", targetChecksum=" + targetChecksum);
        });
        return task(taskNo);
    }

    public Map<String, Object> task(String taskNo) {
        return jdbcTemplate.queryForMap("SELECT * FROM migration_task WHERE task_no=?", taskNo);
    }
    public List<Map<String, Object>> shards(String taskNo) { return jdbcTemplate.queryForList("SELECT * FROM migration_plan_shard WHERE task_no=? ORDER BY shard_no", taskNo); }
    public List<Map<String, Object>> batchLogs(String taskNo) { return jdbcTemplate.queryForList("SELECT * FROM migration_batch_log WHERE task_no=? ORDER BY id DESC LIMIT 500", taskNo); }
    public List<Map<String, Object>> logs(String taskNo) { return jdbcTemplate.queryForList("SELECT * FROM migration_log WHERE task_no=? ORDER BY id DESC LIMIT 200", taskNo); }

    private void updateTaskProgress(String taskNo) {
        jdbcTemplate.update("UPDATE migration_task t SET executed_count=(SELECT COALESCE(SUM(success_count),0) FROM migration_plan_shard s WHERE s.task_no=t.task_no), progress=CASE WHEN total_count=0 THEN 0 ELSE (SELECT COALESCE(SUM(success_count),0) FROM migration_plan_shard s WHERE s.task_no=t.task_no) * 100 / total_count END WHERE task_no=?", taskNo);
    }
    private void log(String taskNo, String level, String stage, String msg) { jdbcTemplate.update("INSERT INTO migration_log(task_no, level, stage, message) VALUES(?,?,?,?)", taskNo, level, stage, msg); }
    private int positive(Integer v, int def) { return v == null || v <= 0 ? def : v; }
    private String rootMsg(Throwable e) { Throwable t=e; while(t.getCause()!=null) t=t.getCause(); return t.getMessage(); }

    private record ShardRange(long startId, long endId, long plannedCount) { }
}
