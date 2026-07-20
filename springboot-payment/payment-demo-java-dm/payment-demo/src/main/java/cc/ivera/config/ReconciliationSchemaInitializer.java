package cc.ivera.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Component
@Slf4j
public class ReconciliationSchemaInitializer implements CommandLineRunner {

    private final JdbcTemplate jdbcTemplate;

    public ReconciliationSchemaInitializer(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    @Override
    public void run(String... args) throws Exception {
        try {
            Integer count = jdbcTemplate.queryForObject(
                    "SELECT COUNT(*) FROM USER_TABLES WHERE TABLE_NAME IN ('T_RECONCILIATION_BATCH','T_RECONCILIATION_DETAIL','T_RECONCILIATION_DISCREPANCY')",
                    Integer.class
            );

            if (count != null && count >= 3) {
                log.info("对账相关表已存在（{}张），跳过初始化", count);
                return;
            }

            log.info("对账相关表不存在（当前{}张），开始执行初始化", count);

            List<String> sqlStatements = buildSqlStatements();
            int success = 0;
            int skipped = 0;
            int failed = 0;

            for (String sql : sqlStatements) {
                try {
                    jdbcTemplate.execute(sql);
                    success++;
                    log.info("  [OK] {}", sql.length() > 60 ? sql.substring(0, 60) + "..." : sql);
                } catch (Exception e) {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("已存在") || msg.contains("already exists") || msg.contains("DUPLICATE") || msg.contains("重复")) {
                        skipped++;
                        log.debug("  [SKIP] {}", msg);
                    } else {
                        failed++;
                        log.error("  [FAIL] SQL: {} | Error: {}", sql.length() > 80 ? sql.substring(0, 80) + "..." : sql, msg);
                    }
                }
            }

            log.info("对账表初始化完成：成功{}条，跳过{}条，失败{}条", success, skipped, failed);
        } catch (Exception e) {
            log.warn("对账表初始化检查失败（不影响主流程）：{}", e.getMessage());
        }
    }

    private List<String> buildSqlStatements() {
        List<String> list = new ArrayList<>();

        list.add("CREATE TABLE t_reconciliation_batch (" +
                "id BIGINT IDENTITY(1, 1) NOT NULL, " +
                "batch_no VARCHAR(50) NOT NULL, " +
                "channel_code VARCHAR(32) NOT NULL, " +
                "payment_app_id BIGINT, " +
                "bill_date VARCHAR(10) NOT NULL, " +
                "status VARCHAR(30) NOT NULL, " +
                "channel_total_count INT DEFAULT 0, " +
                "channel_total_amount INT DEFAULT 0, " +
                "local_total_count INT DEFAULT 0, " +
                "local_total_amount INT DEFAULT 0, " +
                "matched_count INT DEFAULT 0, " +
                "matched_amount INT DEFAULT 0, " +
                "discrepancy_count INT DEFAULT 0, " +
                "overpayment_count INT DEFAULT 0, " +
                "underpayment_count INT DEFAULT 0, " +
                "amount_mismatch_count INT DEFAULT 0, " +
                "status_mismatch_count INT DEFAULT 0, " +
                "failure_reason VARCHAR(512), " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT pk_reconciliation_batch PRIMARY KEY (id), " +
                "CONSTRAINT uk_batch_no UNIQUE (batch_no), " +
                "CONSTRAINT uk_batch_channel_date_app UNIQUE (channel_code, bill_date, payment_app_id))");

        list.add("CREATE INDEX idx_batch_status ON t_reconciliation_batch(status)");
        list.add("CREATE INDEX idx_batch_channel_date ON t_reconciliation_batch(channel_code, bill_date)");

        list.add("CREATE TABLE t_reconciliation_detail (" +
                "id BIGINT IDENTITY(1, 1) NOT NULL, " +
                "batch_no VARCHAR(50) NOT NULL, " +
                "order_no VARCHAR(50), " +
                "transaction_id VARCHAR(50), " +
                "trade_type VARCHAR(20), " +
                "channel_amount INT, " +
                "local_amount INT, " +
                "channel_status VARCHAR(50), " +
                "local_status VARCHAR(50), " +
                "match_status VARCHAR(30), " +
                "discrepancy_type VARCHAR(30), " +
                "trade_time TIMESTAMP, " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT pk_reconciliation_detail PRIMARY KEY (id))");

        list.add("CREATE INDEX idx_detail_batch_no ON t_reconciliation_detail(batch_no)");
        list.add("CREATE INDEX idx_detail_match_status ON t_reconciliation_detail(batch_no, match_status)");
        list.add("CREATE INDEX idx_detail_discrepancy ON t_reconciliation_detail(batch_no, discrepancy_type)");

        list.add("CREATE TABLE t_reconciliation_discrepancy (" +
                "id BIGINT IDENTITY(1, 1) NOT NULL, " +
                "batch_no VARCHAR(50) NOT NULL, " +
                "detail_id BIGINT, " +
                "discrepancy_type VARCHAR(30) NOT NULL, " +
                "status VARCHAR(20) DEFAULT 'OPEN' NOT NULL, " +
                "resolve_remark VARCHAR(512), " +
                "resolved_time TIMESTAMP, " +
                "resolved_by VARCHAR(64), " +
                "create_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "update_time TIMESTAMP DEFAULT CURRENT_TIMESTAMP, " +
                "CONSTRAINT pk_reconciliation_discrepancy PRIMARY KEY (id))");

        list.add("CREATE INDEX idx_discrepancy_batch ON t_reconciliation_discrepancy(batch_no)");
        list.add("CREATE INDEX idx_discrepancy_status ON t_reconciliation_discrepancy(status)");

        return list;
    }
}
