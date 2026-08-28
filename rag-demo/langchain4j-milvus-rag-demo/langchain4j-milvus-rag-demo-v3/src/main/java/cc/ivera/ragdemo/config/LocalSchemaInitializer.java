package cc.ivera.ragdemo.config;

import cc.ivera.ragdemo.service.ragops.SqlCreateTableStatementExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.core.env.Environment;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

@Slf4j
@Component
@Order(Ordered.HIGHEST_PRECEDENCE)
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class LocalSchemaInitializer implements ApplicationRunner {

    private static final String DEMO_TENANT_CODE = "demo";
    private static final String DEMO_TENANT_NAME = "Demo Tenant";
    private static final String DEMO_USER_ID = "demo-user";
    private static final String DEMO_USER_PASSWORD = "a605288582";
    private static final String ADMIN_USER_ID = "admin";
    private static final String ADMIN_PASSWORD = "admin";
    private static final String DEMO_TENANT_ID_SQL = """
            SELECT `id`
            FROM `sys_tenant`
            WHERE `tenant_code` = ?
              AND `is_deleted` = 0
            LIMIT 1
            """;
    private static final String TABLE_EXISTS_SQL = """
            SELECT COUNT(*)
            FROM information_schema.tables
            WHERE table_schema = DATABASE()
              AND table_name = ?
            """;
    private static final String COLUMN_EXISTS_SQL = """
            SELECT COUNT(*)
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = ?
            """;
    private static final String INDEX_EXISTS_SQL = """
            SELECT COUNT(*)
            FROM information_schema.statistics
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND index_name = ?
            """;
    private static final String COLUMN_LENGTH_SQL = """
            SELECT CHARACTER_MAXIMUM_LENGTH
            FROM information_schema.columns
            WHERE table_schema = DATABASE()
              AND table_name = ?
              AND column_name = ?
            """;
    private static final String GLOBAL_AGENT_PROMPT_COUNT_SQL = """
            SELECT COUNT(*)
            FROM `rag_agent_prompt`
            WHERE `tenant_id` = 0
              AND `prompt_name` = 'default'
            """;
    private static final String DEFAULT_AGENT_SYSTEM_PROMPT = """
            你是一个企业级 AI 应用助手，负责多轮对话、知识库问答、工具调用与互联网兜底检索。
            你必须遵守以下规则：

            1. 如果用户询问系统内部工单数量、工单状态、处理进度、状态分布、某时间范围工单统计，
               优先调用 ticketAnalysis 工具。
            2. 当用户要查询某个处理人的工单数量、工单状态、处理情况，但没有提供处理人用户ID时，
               不要直接调用 ticketAnalysis 工具，先追问用户：
               "请告诉我需要查询的处理人用户ID。"
            3. 当用户已明确提供处理人用户ID后，再调用 ticketAnalysis 工具。
            4. 对于业务知识、项目知识、私有文档知识、技术实现类问题，优先调用 knowledgeSearch 工具。
            5. 当 knowledgeSearch 返回未命中、信息不足、或用户问题明确需要最新互联网信息时，再调用 webSearch 工具。
            6. 当用户询问天气、气温、降雨、风力、未来天气时，调用 weatherForecast 工具。
            7. 只有在工具调用所需参数充足时才调用工具；缺少关键参数时先追问。
            8. 工单分析类回答必须包含：
               - 查询范围
               - 总工单数
               - 各状态数量
               - 简要处理情况判断
            9. 如果调用了互联网搜索，请在正文中说明结论，并保留"来源"语义，方便外层系统追加标准来源块。
            10. 对话要简洁、专业、中文输出；必要时可用要点列表。
            11. 如果只是闲聊、问候、解释能力范围，可直接回答，不必强制调用工具。
            """.trim();
    private static final List<ColumnRepair> COLUMN_REPAIRS = List.of(
            ColumnRepair.tenantRequired("work_order", false),
            ColumnRepair.tenantRequired("rag_ingestion_task_shard", true),
            ColumnRepair.tenantRequired("rag_ingestion_task_event", true),
            ColumnRepair.tenantRequired("rag_ingestion_task_stage", true),
            ColumnRepair.tenantRequired("rag_query_hit", true),
            ColumnRepair.tenantRequired("rag_query_feedback", true),
            ColumnRepair.tenantRequired("rag_query_log_delete_audit", true),
            ColumnRepair.tenantRequired("rag_query_feedback_event", true),
            ColumnRepair.tenantRequired("rag_retrieval_eval_case_result", true),
            ColumnRepair.tenantRequired("rag_retrieval_eval_cluster", true),
            ColumnRepair.tenantNullable("sys_admin_impersonation_session"),
            ColumnRepair.tenantNullable("sys_operation_audit_log"),
            ColumnRepair.after("sys_user", "password_hash",
                    "VARCHAR(255) DEFAULT NULL COMMENT 'BCrypt password hash'", "email"),
            ColumnRepair.after("sys_user", "password_updated_at",
                    "DATETIME(3) DEFAULT NULL COMMENT 'Last password update time'", "password_hash"),
            ColumnRepair.after("sys_user", "must_change_password",
                    "TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether user must change password after login'", "password_updated_at"),
            ColumnRepair.after("sys_user_role", "is_deleted",
                    "TINYINT NOT NULL DEFAULT 0 COMMENT 'Soft delete flag'", "created_at"),
            ColumnRepair.after("rag_tenant_model_config", "temperature",
                    "DECIMAL(3,2) DEFAULT 0.20 COMMENT 'LLM temperature (0-2)'", "api_key_secret_ref"),
            ColumnRepair.after("rag_tenant_model_config", "dimension",
                    "INT DEFAULT 1536 COMMENT 'Embedding dimension'", "temperature"),
            ColumnRepair.after("rag_tenant_model_config", "image_size",
                    "VARCHAR(64) DEFAULT NULL COMMENT 'Text-to-image output size'", "dimension"),
            ColumnRepair.after("rag_tenant_model_config", "image_quality",
                    "VARCHAR(64) DEFAULT NULL COMMENT 'Text-to-image quality option'", "image_size"),
            ColumnRepair.after("rag_tenant_model_config", "poll_interval_millis",
                    "INT DEFAULT NULL COMMENT 'Text-to-image polling interval milliseconds'", "image_quality"),
            ColumnRepair.after("rag_tenant_model_config", "rate_limit_qps",
                    "INT DEFAULT NULL COMMENT 'Rate limit (requests per second)'", "poll_interval_millis"),
            ColumnRepair.after("rag_tenant_model_config", "monthly_budget_cents",
                    "BIGINT DEFAULT NULL COMMENT 'Monthly budget in cents'", "rate_limit_qps"),
            ColumnRepair.after("rag_tenant_model_config", "timeout_seconds",
                    "INT DEFAULT NULL COMMENT 'Request timeout seconds'", "monthly_budget_cents"),
            ColumnRepair.after("rag_tenant_model_config", "max_retries",
                    "INT DEFAULT NULL COMMENT 'Maximum retry count'", "timeout_seconds"),
            ColumnRepair.after("rag_tenant_model_config", "max_tokens",
                    "INT DEFAULT NULL COMMENT 'Maximum output tokens'", "max_retries"),
            ColumnRepair.after("rag_tenant_model_config", "frequency_penalty",
                    "DECIMAL(4,2) DEFAULT NULL COMMENT 'Frequency penalty'", "max_tokens"),
            ColumnRepair.after("rag_tenant_model_config", "presence_penalty",
                    "DECIMAL(4,2) DEFAULT NULL COMMENT 'Presence penalty'", "frequency_penalty"),
            ColumnRepair.after("rag_tenant_model_config", "top_p",
                    "DECIMAL(4,2) DEFAULT NULL COMMENT 'Top-p sampling value'", "presence_penalty")
    );
    private static final List<IndexRepair> INDEX_REPAIRS = List.of(
            IndexRepair.tenant("work_order"),
            IndexRepair.tenant("rag_ingestion_task_shard"),
            IndexRepair.tenant("rag_ingestion_task_event"),
            IndexRepair.tenant("rag_ingestion_task_stage"),
            IndexRepair.tenant("rag_query_hit"),
            IndexRepair.tenant("rag_query_feedback"),
            IndexRepair.tenant("rag_query_log_delete_audit"),
            IndexRepair.tenant("rag_query_feedback_event"),
            IndexRepair.tenant("rag_retrieval_eval_case_result"),
            IndexRepair.tenant("rag_retrieval_eval_cluster"),
            IndexRepair.tenant("sys_admin_impersonation_session"),
            IndexRepair.tenant("sys_operation_audit_log")
    );

    private final RagProperties properties;
    private final JdbcOperations jdbcOperations;
    private final ResourceLoader resourceLoader;
    private final Environment environment;

    @Override
    public void run(ApplicationArguments args) throws Exception {
        RagProperties.Schema schema = properties.getSchema();
        if (schema == null || !schema.isAutoInitialize() || productionProfileActive()) {
            return;
        }

        Resource resource = resourceLoader.getResource(schema.getBootstrapLocation());
        if (!resource.exists()) {
            throw new IllegalStateException("Schema bootstrap SQL not found: " + schema.getBootstrapLocation());
        }

        String sql = StreamUtils.copyToString(resource.getInputStream(), StandardCharsets.UTF_8);
        var statements = SqlCreateTableStatementExtractor.extract(sql);
        for (String statement : statements) {
            jdbcOperations.execute(statement);
        }
        int schemaRepairs = repairKnownLocalSchemaGaps();
        log.info("Local schema initialization completed. location={}, createTableStatements={}, schemaRepairs={}",
                schema.getBootstrapLocation(), statements.size(), schemaRepairs);
    }

    private boolean productionProfileActive() {
        return Arrays.stream(environment.getActiveProfiles())
                .map(profile -> profile.toLowerCase(Locale.ROOT))
                .anyMatch(profile -> "prod".equals(profile) || "production".equals(profile));
    }

    private int repairKnownLocalSchemaGaps() {
        int repairs = 0;
        Set<String> repairedColumns = new HashSet<>();
        for (ColumnRepair repair : COLUMN_REPAIRS) {
            if (!exists(TABLE_EXISTS_SQL, repair.table())) {
                continue;
            }
            if (!exists(COLUMN_EXISTS_SQL, repair.table(), repair.column())) {
                jdbcOperations.execute("""
                        ALTER TABLE `%s`
                            ADD COLUMN `%s` %s AFTER `%s`
                        """.formatted(repair.table(), repair.column(), repair.columnDefinition(), repair.afterColumn()).trim());
                repairedColumns.add(repair.key());
                repairs++;
            }
        }
        for (IndexRepair repair : INDEX_REPAIRS) {
            if (!exists(TABLE_EXISTS_SQL, repair.table())) {
                continue;
            }
            if (!repairedColumns.contains(repair.columnKey()) && !exists(COLUMN_EXISTS_SQL, repair.table(), repair.column())) {
                continue;
            }
            if (!exists(INDEX_EXISTS_SQL, repair.table(), repair.index())) {
                jdbcOperations.execute("ALTER TABLE `%s` ADD KEY `%s` (`%s`)"
                        .formatted(repair.table(), repair.index(), repair.column()));
                repairs++;
            }
        }
        repairs += repairTenantModelApiKeyColumnLength();
        repairs += seedGlobalAgentPromptIfMissing();
        repairs += seedLocalLoginAccountsIfMissing();
        return repairs;
    }

    private int repairTenantModelApiKeyColumnLength() {
        if (!exists(TABLE_EXISTS_SQL, "rag_tenant_model_config")
                || !exists(COLUMN_EXISTS_SQL, "rag_tenant_model_config", "api_key_secret_ref")) {
            return 0;
        }
        Long length = jdbcOperations.queryForObject(
                COLUMN_LENGTH_SQL,
                Long.class,
                "rag_tenant_model_config",
                "api_key_secret_ref"
        );
        if (length != null && length >= 2048) {
            return 0;
        }
        jdbcOperations.execute("""
                ALTER TABLE `rag_tenant_model_config`
                    MODIFY COLUMN `api_key_secret_ref` VARCHAR(2048) DEFAULT NULL
                """.trim());
        return 1;
    }

    private int seedGlobalAgentPromptIfMissing() {
        if (!exists(TABLE_EXISTS_SQL, "rag_agent_prompt")
                || !exists(COLUMN_EXISTS_SQL, "rag_agent_prompt", "prompt_content")) {
            return 0;
        }
        Integer count = jdbcOperations.queryForObject(GLOBAL_AGENT_PROMPT_COUNT_SQL, Integer.class);
        if (count != null && count > 0) {
            return 0;
        }
        jdbcOperations.update("""
                INSERT INTO `rag_agent_prompt` (`tenant_id`, `prompt_name`, `prompt_content`, `version`, `status`, `created_by`)
                VALUES (0, 'default', ?, 1, 1, 'system')
                """.trim(), DEFAULT_AGENT_SYSTEM_PROMPT);
        return 1;
    }

    private int seedLocalLoginAccountsIfMissing() {
        PasswordEncoder encoder = new BCryptPasswordEncoder();
        int repairs = 0;
        if (platformAdminTableReady()) {
            repairs += upsertPlatformAdmin(
                ADMIN_USER_ID,
                "Super Administrator",
                "admin@example.local",
                encoder.encode(ADMIN_PASSWORD)
            );
        }
        if (tenantAuthTablesReady()) {
            repairs += upsertLocalTenant();
            Long demoTenantId = jdbcOperations.queryForObject(DEMO_TENANT_ID_SQL, Long.class, DEMO_TENANT_CODE);
            if (demoTenantId != null) {
                repairs += upsertLocalRole(demoTenantId, "TENANT_ADMIN", "Tenant Administrator", "TENANT");
                repairs += upsertLocalRole(demoTenantId, "KB_OWNER", "Knowledge Base Owner", "TENANT");
                repairs += upsertLocalUser(
                        demoTenantId,
                        DEMO_USER_ID,
                        DEMO_USER_ID,
                        "Demo User",
                        "demo-user@example.local",
                        encoder.encode(DEMO_USER_PASSWORD)
                );
                repairs += upsertLocalUserRole(demoTenantId, DEMO_USER_ID, "TENANT_ADMIN");
                repairs += upsertLocalUserRole(demoTenantId, DEMO_USER_ID, "KB_OWNER");
            }
        }
        return repairs;
    }

    private boolean tenantAuthTablesReady() {
        return exists(TABLE_EXISTS_SQL, "sys_tenant")
                && exists(TABLE_EXISTS_SQL, "sys_user")
                && exists(TABLE_EXISTS_SQL, "sys_role")
                && exists(TABLE_EXISTS_SQL, "sys_user_role")
                && exists(COLUMN_EXISTS_SQL, "sys_user", "password_hash")
                && exists(COLUMN_EXISTS_SQL, "sys_user", "password_updated_at")
                && exists(COLUMN_EXISTS_SQL, "sys_user", "must_change_password")
                && exists(COLUMN_EXISTS_SQL, "sys_user_role", "is_deleted");
    }

    private boolean platformAdminTableReady() {
        return exists(TABLE_EXISTS_SQL, "sys_platform_admin")
                && exists(COLUMN_EXISTS_SQL, "sys_platform_admin", "singleton_key")
                && exists(COLUMN_EXISTS_SQL, "sys_platform_admin", "admin_username")
                && exists(COLUMN_EXISTS_SQL, "sys_platform_admin", "password_hash")
                && exists(COLUMN_EXISTS_SQL, "sys_platform_admin", "password_updated_at")
                && exists(COLUMN_EXISTS_SQL, "sys_platform_admin", "must_change_password")
                && exists(COLUMN_EXISTS_SQL, "sys_platform_admin", "status")
                && exists(COLUMN_EXISTS_SQL, "sys_platform_admin", "is_deleted");
    }

    private int upsertLocalTenant() {
        return jdbcOperations.update("""
                INSERT INTO `sys_tenant` (`tenant_code`, `tenant_name`, `external_id`, `status`, `is_deleted`)
                VALUES (?, ?, ?, 1, 0)
                ON DUPLICATE KEY UPDATE
                    `tenant_name` = VALUES(`tenant_name`),
                    `external_id` = VALUES(`external_id`),
                    `status` = 1,
                    `is_deleted` = 0,
                    `updated_at` = CURRENT_TIMESTAMP(3)
                """.trim(), DEMO_TENANT_CODE, DEMO_TENANT_NAME, DEMO_TENANT_CODE);
    }

    private int upsertPlatformAdmin(String adminUsername,
                                    String displayName,
                                    String email,
                                    String passwordHash) {
        return jdbcOperations.update("""
                INSERT INTO `sys_platform_admin` (
                    `singleton_key`,
                    `admin_username`,
                    `display_name`,
                    `email`,
                    `password_hash`,
                    `password_updated_at`,
                    `must_change_password`,
                    `status`,
                    `is_deleted`
                )
                VALUES (1, ?, ?, ?, ?, CURRENT_TIMESTAMP(3), 0, 1, 0)
                ON DUPLICATE KEY UPDATE
                    `admin_username` = VALUES(`admin_username`),
                    `display_name` = VALUES(`display_name`),
                    `email` = VALUES(`email`),
                    `password_updated_at` = IF(`password_hash` IS NULL OR `password_hash` = '', VALUES(`password_updated_at`), `password_updated_at`),
                    `must_change_password` = IF(`password_hash` IS NULL OR `password_hash` = '', 0, `must_change_password`),
                    `password_hash` = IF(`password_hash` IS NULL OR `password_hash` = '', VALUES(`password_hash`), `password_hash`),
                    `status` = 1,
                    `is_deleted` = 0,
                    `updated_at` = CURRENT_TIMESTAMP(3)
                """.trim(), adminUsername, displayName, email, passwordHash);
    }

    private int upsertLocalRole(Long tenantId, String roleCode, String roleName, String roleScope) {
        return jdbcOperations.update("""
                INSERT INTO `sys_role` (`tenant_id`, `role_code`, `role_name`, `role_scope`, `is_deleted`)
                VALUES (?, ?, ?, ?, 0)
                ON DUPLICATE KEY UPDATE
                    `role_name` = VALUES(`role_name`),
                    `role_scope` = VALUES(`role_scope`),
                    `is_deleted` = 0,
                    `updated_at` = CURRENT_TIMESTAMP(3)
                """.trim(), tenantId, roleCode, roleName, roleScope);
    }

    private int upsertLocalUser(Long tenantId,
                                String externalUserId,
                                String username,
                                String displayName,
                                String email,
                                String passwordHash) {
        return jdbcOperations.update("""
                INSERT INTO `sys_user` (
                    `tenant_id`,
                    `external_user_id`,
                    `username`,
                    `display_name`,
                    `email`,
                    `password_hash`,
                    `password_updated_at`,
                    `must_change_password`,
                    `status`,
                    `is_deleted`
                )
                VALUES (?, ?, ?, ?, ?, ?, CURRENT_TIMESTAMP(3), 0, 1, 0)
                ON DUPLICATE KEY UPDATE
                    `username` = VALUES(`username`),
                    `display_name` = VALUES(`display_name`),
                    `email` = VALUES(`email`),
                    `password_updated_at` = IF(`password_hash` IS NULL OR `password_hash` = '', VALUES(`password_updated_at`), `password_updated_at`),
                    `must_change_password` = IF(`password_hash` IS NULL OR `password_hash` = '', 0, `must_change_password`),
                    `password_hash` = IF(`password_hash` IS NULL OR `password_hash` = '', VALUES(`password_hash`), `password_hash`),
                    `status` = 1,
                    `is_deleted` = 0,
                    `updated_at` = CURRENT_TIMESTAMP(3)
                """.trim(), tenantId, externalUserId, username, displayName, email, passwordHash);
    }

    private int upsertLocalUserRole(Long tenantId, String userId, String roleCode) {
        return jdbcOperations.update("""
                INSERT INTO `sys_user_role` (`tenant_id`, `user_id`, `role_code`, `is_deleted`)
                VALUES (?, ?, ?, 0)
                ON DUPLICATE KEY UPDATE
                    `is_deleted` = 0
                """.trim(), tenantId, userId, roleCode);
    }

    private boolean exists(String sql, String table, String name) {
        Integer count = jdbcOperations.queryForObject(sql, Integer.class, table, name);
        return count != null && count > 0;
    }

    private boolean exists(String sql, String table) {
        Integer count = jdbcOperations.queryForObject(sql, Integer.class, table);
        return count != null && count > 0;
    }

    private record ColumnRepair(String table, String column, String columnDefinition, String afterColumn) {

        private static ColumnRepair tenantRequired(String table, boolean unsigned) {
            return after(table, "tenant_id",
                    "BIGINT" + (unsigned ? " UNSIGNED" : "") + " NOT NULL DEFAULT 0 COMMENT 'Tenant ID'",
                    "id");
        }

        private static ColumnRepair tenantNullable(String table) {
            return after(table, "tenant_id", "BIGINT UNSIGNED DEFAULT NULL COMMENT 'Tenant ID'", "id");
        }

        private static ColumnRepair after(String table, String column, String columnDefinition, String afterColumn) {
            return new ColumnRepair(table, column, columnDefinition, afterColumn);
        }

        private String key() {
            return table + "." + column;
        }
    }

    private record IndexRepair(String table, String index, String column) {

        private static IndexRepair tenant(String table) {
            return new IndexRepair(table, "idx_tenant_id", "tenant_id");
        }

        private String columnKey() {
            return table + "." + column;
        }
    }
}
