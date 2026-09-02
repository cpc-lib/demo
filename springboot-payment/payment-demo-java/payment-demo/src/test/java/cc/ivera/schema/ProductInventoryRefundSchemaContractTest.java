package cc.ivera.schema;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ProductInventoryRefundSchemaContractTest {

    private static String sql;

    @BeforeAll
    static void readConsolidatedSql() throws Exception {
        sql = new String(
                Files.readAllBytes(Paths.get("sql", "payment-demo.sql")),
                StandardCharsets.UTF_8
        ).toLowerCase();
    }

    @Test
    void consolidatedSqlContainsProductAndRefundInventoryColumns() {
        String productTable = tableDefinition("t_product");
        String orderItemTable = tableDefinition("t_order_item");
        String refundTable = tableDefinition("t_refund_info");

        assertTrue(productTable.contains("`status` varchar(16) character set utf8mb4 collate utf8mb4_general_ci not null default 'off_shelf'"));
        assertTrue(productTable.contains("`available_stock` int(11) unsigned not null default 0"));
        assertTrue(productTable.contains("`locked_stock` int(11) unsigned not null default 0"));
        assertTrue(productTable.contains("`sold_stock` int(11) unsigned not null default 0"));
        assertTrue(productTable.contains("`version` int(11) not null default 0"));
        assertTrue(orderItemTable.contains("`inventory_status` varchar(16) character set utf8mb4 collate utf8mb4_general_ci not null default 'reserved'"));
        assertTrue(orderItemTable.contains("`refunded_quantity` int(11) unsigned not null default 0"));
        assertTrue(refundTable.contains("`application_request_id` varchar(64) character set utf8mb4 collate utf8mb4_general_ci not null"));
    }

    @Test
    void consolidatedSqlContainsIdempotencyInventoryRefundAndMessageTables() {
        tableDefinition("t_order_idempotency");
        tableDefinition("t_refund_item");
        tableDefinition("t_inventory_operation");
        tableDefinition("t_message_outbox");
        tableDefinition("t_message_consume_log");
    }

    @Test
    void consolidatedSqlContainsRequiredUniqueKeysAndInventoryOperationTypes() {
        String orderIdempotencyTable = tableDefinition("t_order_idempotency");
        String refundTable = tableDefinition("t_refund_info");
        String inventoryTable = tableDefinition("t_inventory_operation");
        String outboxTable = tableDefinition("t_message_outbox");
        String consumeLogTable = tableDefinition("t_message_consume_log");

        assertTrue(orderIdempotencyTable.contains("unique key `uk_order_idempotency_key` (`idempotency_key`)"));
        assertTrue(orderIdempotencyTable.contains("unique key `uk_order_idempotency_order` (`order_id`)"));
        assertTrue(orderIdempotencyTable.contains("foreign key (`user_id`) references `t_user` (`id`)"));
        assertTrue(orderIdempotencyTable.contains("foreign key (`order_id`) references `t_order_info` (`id`)"));
        assertTrue(refundTable.contains("unique key `uk_refund_order_request` (`order_no`, `application_request_id`)"));
        assertTrue(inventoryTable.contains("unique key `uk_inventory_business_key` (`business_key`)"));
        assertTrue(inventoryTable.contains("foreign key (`product_id`) references `t_product` (`id`)"));
        assertTrue(outboxTable.contains("unique key `uk_outbox_event_key` (`event_key`)"));
        assertTrue(consumeLogTable.contains("unique key `uk_consume_event_consumer` (`event_id`, `consumer_name`)"));
        assertTrue(inventoryTable.contains("admin_adjust"));
        assertTrue(inventoryTable.contains("order_reserve"));
        assertTrue(inventoryTable.contains("order_commit"));
        assertTrue(inventoryTable.contains("order_release"));
        assertTrue(inventoryTable.contains("refund_restore"));
    }

    @Test
    void productTotalStockIsDerivedInsteadOfStored() {
        assertFalse(tableDefinition("t_product").contains("`total_stock`"));
        assertTrue(sql.contains("available_stock + locked_stock + sold_stock"));
    }

    @Test
    void runtimeConfigurationUses120SecondBackendOrderKeyTtl() throws Exception {
        String applicationYaml = new String(
                Files.readAllBytes(Paths.get("src", "main", "resources", "application.yml")),
                StandardCharsets.UTF_8
        ).replace("\r\n", "\n");

        assertTrue(applicationYaml.contains(
                "  order:\n"
                        + "    close-delay-ms: 60000\n"
                        + "    idempotency-key-ttl-seconds: 120\n"
        ));
    }

    private static String tableDefinition(String tableName) {
        String marker = "create table `" + tableName + "`";
        int start = sql.indexOf(marker);
        assertTrue(start >= 0, "missing table " + tableName);
        int end = sql.indexOf(") engine", start);
        assertTrue(end > start, "unterminated table " + tableName);
        return sql.substring(start, end);
    }
}
