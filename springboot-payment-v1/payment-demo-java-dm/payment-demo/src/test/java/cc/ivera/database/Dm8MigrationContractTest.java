package cc.ivera.database;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.assertj.core.api.Assertions.assertThat;

class Dm8MigrationContractTest {

    @Test
    @DisplayName("DM8迁移契约: Maven依赖使用达梦驱动且不再依赖MySQL驱动")
    void dm8MigrationUsesDamengJdbcDriver() throws IOException {
        String pom = read("pom.xml");

        assertThat(pom).contains("<groupId>com.dameng</groupId>");
        assertThat(pom).contains("<artifactId>DmJdbcDriver18</artifactId>");
        assertThat(pom).doesNotContain("mysql-connector-java");
        assertThat(pom).doesNotContain("<groupId>mysql</groupId>");
    }

    @Test
    @DisplayName("DM8迁移契约: Spring数据源默认连接达梦")
    void dm8MigrationUsesDamengDatasource() throws IOException {
        String application = read("src/main/resources/application.yml");

        assertThat(application).contains("driver-class-name: dm.jdbc.driver.DmDriver");
        assertThat(application).contains("url: jdbc:dm://${DM_HOST:192.168.220.200}:${DM_PORT:5236}?schema=${DM_SCHEMA:SYSDBA}&connectTimeout=30000");
        assertThat(application).contains("username: ${DM_USERNAME:SYSDBA}");
        assertThat(application).contains("password: ${DM_PASSWORD:Cpc2026#@Dm}");
        assertThat(application).doesNotContain("jdbc:mysql://");
        assertThat(application).doesNotContain("com.mysql.jdbc.Driver");
    }

    @Test
    @DisplayName("DM8迁移契约: Mapper SQL 不再使用 MySQL limit/ifnull 方言")
    void mapperSqlDoesNotUseMysqlDialect() throws IOException {
        String orderMapper = read("src/main/resources/mapper/OrderInfoMapper.xml");
        String productMapper = read("src/main/resources/mapper/ProductMapper.xml");
        String refundMapper = read("src/main/resources/mapper/RefundInfoMapper.xml");

        assertThat(orderMapper).doesNotContain("limit 1");
        assertThat(productMapper).doesNotContain("limit 1");
        assertThat(refundMapper).doesNotContain("limit 1");
        assertThat(refundMapper).doesNotContain("ifnull(");
        assertThat(refundMapper).contains("coalesce(sum(refund), 0)");
        assertThat(orderMapper).contains("rownum &lt;= 1");
    }

    @Test
    @DisplayName("DM8迁移契约: 初始化DDL使用达梦语法且不包含MySQL建表子句")
    void dm8SchemaScriptDoesNotUseMysqlDdl() throws IOException {
        String ddl = read("env/sql/dm8/001_payment_demo_dm8.sql").toLowerCase();

        assertThat(ddl).contains("identity(1, 1)");
        assertThat(ddl).contains("clob");
        assertThat(ddl).contains("create or replace trigger");
        assertThat(ddl).contains("from user_tables");
        assertThat(ddl).contains("execute immediate 'drop table t_payment_channel'");
        assertThat(ddl).doesNotContain("auto_increment");
        assertThat(ddl).doesNotContain("engine = innodb");
        assertThat(ddl).doesNotContain("utf8mb4");
        assertThat(ddl).doesNotContain("on update current_timestamp");
        assertThat(ddl).doesNotContain("json null");
        assertThat(ddl).doesNotContain("drop table if exists");
        assertThat(ddl).doesNotContain("compatible_mode");
        assertThat(ddl).doesNotContain("`t_");
    }

    @Test
    @DisplayName("DM8 migration contract: startup payment config tables match entity mappings")
    void paymentConfigStartupTablesMatchEntityMappings() throws IOException {
        String ddl = read("env/sql/dm8/001_payment_demo_dm8.sql").toLowerCase();
        String paymentChannelEntity = read("src/main/java/cc/ivera/entity/PaymentChannel.java");
        String paymentAppEntity = read("src/main/java/cc/ivera/entity/PaymentApp.java");

        assertThat(ddl).contains("create table t_payment_channel");
        assertThat(ddl).contains("create table t_payment_app");
        assertThat(ddl).contains("create table t_product");
        assertThat(ddl).contains("create table t_order_info");
        assertThat(ddl).contains("create table t_payment_info");
        assertThat(ddl).contains("create table t_refund_info");
        assertThat(ddl).contains("insert into t_payment_channel");
        assertThat(ddl).contains("insert into t_payment_app");
        assertThat(ddl).contains("insert into t_product");
        assertThat(paymentChannelEntity).contains("@TableName(\"t_payment_channel\")");
        assertThat(paymentAppEntity).contains("@TableName(\"t_payment_app\")");
    }

    @Test
    @DisplayName("DM8迁移契约: docker-compose 和初始化脚本存在")
    void dm8DockerComposeAndScriptsExist() throws IOException {
        String compose = read("env/docker-compose.dm8.yml");
        String initScript = read("env/scripts/dm8/init-dm8-sql.sh");
        String composeLowerCase = compose.toLowerCase();
        String initScriptLowerCase = initScript.toLowerCase();

        assertThat(compose).contains("registry.cn-hangzhou.aliyuncs.com/snow-io/dm8:latest");
        assertThat(compose).contains("SYSDBA_PWD");
        assertThat(compose).contains("./sql/dm8:/dm8-init:ro");
        assertThat(compose).contains("${DM_PORT:-5236}:5236");
        assertThat(initScript).contains("/opt/dmdbms/bin/disql");
        assertThat(initScript).contains("SQL_DIR=\"${DM_SQL_DIR:-/dm8-init}\"");
        assertThat(initScript).contains("start ${sql_file}");
        assertThat(initScript).contains("PAYMENT_DEMO_REQUIRED_TABLE_COUNT=6");
        assertThat(initScript).contains("select table_name from user_tables");
        assertThat(initScript).contains("T_PAYMENT_CHANNEL");
        assertThat(initScript).contains("T_PAYMENT_APP");
        assertThat(composeLowerCase).doesNotContain("compatible_mode");
        assertThat(initScriptLowerCase).doesNotContain("compatible_mode");
    }

    private String read(String relativePath) throws IOException {
        Path path = Paths.get(relativePath);
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8);
    }
}
