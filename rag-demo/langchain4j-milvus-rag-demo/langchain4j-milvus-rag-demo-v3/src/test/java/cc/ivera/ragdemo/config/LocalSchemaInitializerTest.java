package cc.ivera.ragdemo.config;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.jdbc.core.JdbcOperations;
import org.springframework.mock.env.MockEnvironment;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mockito.invocation.Invocation;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class LocalSchemaInitializerTest {

    @Test
    void initializesCreateTableStatementsWhenEnabledOutsideProductionProfile() throws Exception {
        RagProperties properties = new RagProperties();
        properties.getSchema().setAutoInitialize(true);
        properties.getSchema().setBootstrapLocation("classpath:test-schema.sql");
        JdbcOperations jdbcOperations = mock(JdbcOperations.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getResource("classpath:test-schema.sql")).thenReturn(sqlResource("""
                CREATE TABLE `work_order` (`id` BIGINT NOT NULL);
                CREATE TABLE IF NOT EXISTS `rag_knowledge_base` (`id` BIGINT NOT NULL);
                ALTER TABLE `rag_knowledge_base` ADD COLUMN `name` VARCHAR(128);
                """));

        new LocalSchemaInitializer(properties, jdbcOperations, resourceLoader, new MockEnvironment()).run(null);

        verify(jdbcOperations).execute("CREATE TABLE IF NOT EXISTS `work_order` (`id` BIGINT NOT NULL)");
        verify(jdbcOperations).execute("CREATE TABLE IF NOT EXISTS `rag_knowledge_base` (`id` BIGINT NOT NULL)");
        verify(jdbcOperations, times(2)).execute(anyString());
    }

    @Test
    void skipsInitializationForProductionProfile() throws Exception {
        RagProperties properties = new RagProperties();
        properties.getSchema().setAutoInitialize(true);
        JdbcOperations jdbcOperations = mock(JdbcOperations.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        MockEnvironment environment = new MockEnvironment();
        environment.setActiveProfiles("prod");

        new LocalSchemaInitializer(properties, jdbcOperations, resourceLoader, environment).run(null);

        verifyNoInteractions(jdbcOperations, resourceLoader);
    }

    @Test
    void repairsMissingTenantColumnsForExistingLocalTables() throws Exception {
        RagProperties properties = new RagProperties();
        properties.getSchema().setAutoInitialize(true);
        properties.getSchema().setBootstrapLocation("classpath:test-schema.sql");
        JdbcOperations jdbcOperations = mock(JdbcOperations.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getResource("classpath:test-schema.sql")).thenReturn(sqlResource("""
                CREATE TABLE IF NOT EXISTS `rag_query_feedback` (`id` BIGINT UNSIGNED NOT NULL);
                """));
        mockExistingSchema(jdbcOperations, Map.of("rag_query_feedback", Set.of("id")), Map.of());

        new LocalSchemaInitializer(properties, jdbcOperations, resourceLoader, new MockEnvironment()).run(null);

        verify(jdbcOperations).execute("""
                ALTER TABLE `rag_query_feedback`
                    ADD COLUMN `tenant_id` BIGINT UNSIGNED NOT NULL DEFAULT 0 COMMENT 'Tenant ID' AFTER `id`
                """.trim());
        verify(jdbcOperations).execute("ALTER TABLE `rag_query_feedback` ADD KEY `idx_tenant_id` (`tenant_id`)");
    }

    @Test
    void repairsMissingModelConfigRuntimeColumnsForExistingLocalTables() throws Exception {
        RagProperties properties = new RagProperties();
        properties.getSchema().setAutoInitialize(true);
        properties.getSchema().setBootstrapLocation("classpath:test-schema.sql");
        JdbcOperations jdbcOperations = mock(JdbcOperations.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getResource("classpath:test-schema.sql")).thenReturn(sqlResource("""
                CREATE TABLE IF NOT EXISTS `rag_tenant_model_config` (`id` BIGINT UNSIGNED NOT NULL);
                """));
        mockExistingSchema(jdbcOperations, Map.of("rag_tenant_model_config", Set.of("id", "tenant_id")), Map.of());

        new LocalSchemaInitializer(properties, jdbcOperations, resourceLoader, new MockEnvironment()).run(null);

        verify(jdbcOperations).execute("""
                ALTER TABLE `rag_tenant_model_config`
                    ADD COLUMN `temperature` DECIMAL(3,2) DEFAULT 0.20 COMMENT 'LLM temperature (0-2)' AFTER `api_key_secret_ref`
                """.trim());
        verify(jdbcOperations).execute("""
                ALTER TABLE `rag_tenant_model_config`
                    ADD COLUMN `top_p` DECIMAL(4,2) DEFAULT NULL COMMENT 'Top-p sampling value' AFTER `presence_penalty`
                """.trim());
    }

    @Test
    void repairsMissingUserPasswordColumnsForExistingLocalTables() throws Exception {
        RagProperties properties = new RagProperties();
        properties.getSchema().setAutoInitialize(true);
        properties.getSchema().setBootstrapLocation("classpath:test-schema.sql");
        JdbcOperations jdbcOperations = mock(JdbcOperations.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getResource("classpath:test-schema.sql")).thenReturn(sqlResource("""
                CREATE TABLE IF NOT EXISTS `sys_user` (`id` BIGINT UNSIGNED NOT NULL);
                """));
        mockExistingSchema(jdbcOperations, Map.of("sys_user", Set.of("id", "tenant_id", "external_user_id", "email")), Map.of());

        new LocalSchemaInitializer(properties, jdbcOperations, resourceLoader, new MockEnvironment()).run(null);

        verify(jdbcOperations).execute("""
                ALTER TABLE `sys_user`
                    ADD COLUMN `password_hash` VARCHAR(255) DEFAULT NULL COMMENT 'BCrypt password hash' AFTER `email`
                """.trim());
        verify(jdbcOperations).execute("""
                ALTER TABLE `sys_user`
                    ADD COLUMN `password_updated_at` DATETIME(3) DEFAULT NULL COMMENT 'Last password update time' AFTER `password_hash`
                """.trim());
        verify(jdbcOperations).execute("""
                ALTER TABLE `sys_user`
                    ADD COLUMN `must_change_password` TINYINT NOT NULL DEFAULT 0 COMMENT 'Whether user must change password after login' AFTER `password_updated_at`
                """.trim());
    }

    @Test
    void seedsGlobalAgentPromptWhenPromptTableExistsButDefaultPromptIsMissing() throws Exception {
        RagProperties properties = new RagProperties();
        properties.getSchema().setAutoInitialize(true);
        properties.getSchema().setBootstrapLocation("classpath:test-schema.sql");
        JdbcOperations jdbcOperations = mock(JdbcOperations.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getResource("classpath:test-schema.sql")).thenReturn(sqlResource("""
                CREATE TABLE IF NOT EXISTS `rag_agent_prompt` (`id` BIGINT UNSIGNED NOT NULL);
                """));
        mockExistingSchema(jdbcOperations, Map.of("rag_agent_prompt", Set.of(
                "id",
                "tenant_id",
                "prompt_name",
                "prompt_content",
                "version",
                "status",
                "created_by"
        )), Map.of());
        when(jdbcOperations.queryForObject(contains("FROM `rag_agent_prompt` WHERE"), eq(Integer.class)))
                .thenReturn(0);

        new LocalSchemaInitializer(properties, jdbcOperations, resourceLoader, new MockEnvironment()).run(null);

        verify(jdbcOperations).update(
                contains("INSERT INTO `rag_agent_prompt`"),
                contains("企业级 AI 应用助手")
        );
    }

    @Test
    void seedsLocalLoginUsersAndRolesWhenAuthTablesExist() throws Exception {
        RagProperties properties = new RagProperties();
        properties.getSchema().setAutoInitialize(true);
        properties.getSchema().setBootstrapLocation("classpath:test-schema.sql");
        JdbcOperations jdbcOperations = mock(JdbcOperations.class);
        ResourceLoader resourceLoader = mock(ResourceLoader.class);
        when(resourceLoader.getResource("classpath:test-schema.sql")).thenReturn(sqlResource("-- no-op"));
        mockExistingSchema(jdbcOperations, Map.of(
                "sys_tenant", Set.of(
                        "id",
                        "tenant_code",
                        "tenant_name",
                        "external_id",
                        "status",
                        "is_deleted"
                ),
                "sys_platform_admin", Set.of(
                        "id",
                        "singleton_key",
                        "admin_username",
                        "display_name",
                        "email",
                        "password_hash",
                        "password_updated_at",
                        "must_change_password",
                        "status",
                        "is_deleted"
                ),
                "sys_user", Set.of(
                        "id",
                        "tenant_id",
                        "external_user_id",
                        "username",
                        "display_name",
                        "email",
                        "password_hash",
                        "password_updated_at",
                        "must_change_password",
                        "status",
                        "is_deleted"
                ),
                "sys_role", Set.of("id", "tenant_id", "role_code", "role_name", "role_scope", "is_deleted"),
                "sys_user_role", Set.of("id", "tenant_id", "user_id", "role_code", "is_deleted")
        ), Map.of());
        when(jdbcOperations.queryForObject(contains("FROM `sys_tenant`"), eq(Long.class), eq("demo")))
                .thenReturn(7L);

        new LocalSchemaInitializer(properties, jdbcOperations, resourceLoader, new MockEnvironment()).run(null);

        List<Invocation> updates = updateInvocations(jdbcOperations);
        assertThat(updates).anySatisfy(invocation -> {
            String arguments = Arrays.deepToString(invocation.getArguments());
            assertThat(arguments).contains("INSERT INTO `sys_user`")
                    .contains("demo-user")
                    .contains("$2a$")
                    .doesNotContain("a605288582");
        });
        assertThat(updates).anySatisfy(invocation -> {
            String arguments = Arrays.deepToString(invocation.getArguments());
            assertThat(arguments).contains("INSERT INTO `sys_platform_admin`")
                    .contains("admin")
                    .contains("$2a$")
                    .doesNotContain("admin]");
        });
        assertThat(updates).anySatisfy(invocation -> assertThat(Arrays.deepToString(invocation.getArguments()))
                .contains("INSERT INTO `sys_role`")
                .contains("TENANT_ADMIN"));
        assertThat(updates).anySatisfy(invocation -> assertThat(Arrays.deepToString(invocation.getArguments()))
                .contains("INSERT INTO `sys_user_role`")
                .contains("demo-user")
                .contains("TENANT_ADMIN"));
        assertThat(updates).noneSatisfy(invocation -> assertThat(Arrays.deepToString(invocation.getArguments()))
                .contains("INSERT INTO `sys_user`")
                .contains("admin"));
    }

    private Resource sqlResource(String sql) {
        return new ByteArrayResource(sql.getBytes(StandardCharsets.UTF_8));
    }

    private List<Invocation> updateInvocations(JdbcOperations jdbcOperations) {
        return mockingDetails(jdbcOperations).getInvocations()
                .stream()
                .filter(invocation -> "update".equals(invocation.getMethod().getName()))
                .toList();
    }

    private void mockExistingSchema(JdbcOperations jdbcOperations,
                                    Map<String, Set<String>> columnsByTable,
                                    Map<String, Set<String>> indexesByTable) {
        var answer = (org.mockito.stubbing.Answer<Integer>) invocation -> {
            String sql = invocation.getArgument(0);
            String table = invocation.getArgument(2);
            if (sql.contains("information_schema.tables")) {
                return columnsByTable.containsKey(table) ? 1 : 0;
            }
            if (sql.contains("information_schema.columns")) {
                String column = invocation.getArgument(3);
                return columnsByTable.getOrDefault(table, Set.of()).contains(column) ? 1 : 0;
            }
            if (sql.contains("information_schema.statistics")) {
                String index = invocation.getArgument(3);
                return indexesByTable.getOrDefault(table, Set.of()).contains(index) ? 1 : 0;
            }
            return 0;
        };
        when(jdbcOperations.queryForObject(anyString(), eq(Integer.class), any())).thenAnswer(answer);
        when(jdbcOperations.queryForObject(anyString(), eq(Integer.class), any(), any())).thenAnswer(answer);
    }
}
