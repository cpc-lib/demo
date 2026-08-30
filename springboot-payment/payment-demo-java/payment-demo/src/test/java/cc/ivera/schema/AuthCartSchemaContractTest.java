package cc.ivera.schema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AuthCartSchemaContractTest {

    @Test
    void consolidatedSqlContainsAuthenticationCartAndOrderItemSchema() throws Exception {
        String sql = new String(
                Files.readAllBytes(Paths.get("sql", "payment-demo.sql")),
                StandardCharsets.UTF_8
        ).toLowerCase();

        assertTrue(sql.contains("create table `t_user`"));
        assertTrue(sql.contains("unique key `uk_user_username` (`username`)"));
        assertTrue(sql.contains("create table `t_refresh_token`"));
        assertTrue(sql.contains("unique key `uk_refresh_token_hash` (`token_hash`)"));
        assertTrue(sql.contains("key `idx_refresh_user_family` (`user_id`, `token_family`)"));
        assertTrue(sql.contains("create table `t_cart`"));
        assertTrue(sql.contains("unique key `uk_cart_user` (`user_id`)"));
        assertTrue(sql.contains("create table `t_cart_item`"));
        assertTrue(sql.contains("unique key `uk_cart_product` (`cart_id`, `product_id`)"));
        assertTrue(sql.contains("create table `t_order_item`"));
        assertTrue(sql.contains("`checkout_request_id` varchar(64)"));
        assertTrue(sql.contains("unique key `uk_order_user_checkout` (`user_id`, `checkout_request_id`)"));
        assertTrue(sql.contains("key `idx_order_user_time` (`user_id`, `create_time`)"));
        assertTrue(sql.contains("`product_id` bigint(20) null default null"));
    }

    @Test
    void demoAdminSeedContainsBcryptHashButNeverPlaintextPassword() throws Exception {
        String sql = new String(
                Files.readAllBytes(Paths.get("sql", "payment-demo.sql")),
                StandardCharsets.UTF_8
        );

        assertTrue(sql.contains("'admin'"));
        assertTrue(sql.matches("(?s).*\\$2[aby]\\$\\d{2}\\$[./A-Za-z0-9]{53}.*"));
        assertFalse(sql.contains("Admin@123456"));
    }
}
