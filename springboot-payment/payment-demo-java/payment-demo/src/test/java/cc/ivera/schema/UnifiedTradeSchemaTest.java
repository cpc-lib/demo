package cc.ivera.schema;

import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class UnifiedTradeSchemaTest {

    private static String schema;
    private static String expansion;
    private static String verification;

    @BeforeAll
    static void readSqlContracts() throws Exception {
        schema = read(Paths.get("sql", "payment-demo.sql"));

        Path expansionPath = Paths.get("sql", "migrations", "20260904_v060_expand.sql");
        Path verificationPath = Paths.get("sql", "migrations", "20260904_v060_verify.sql");
        assertTrue(Files.isRegularFile(expansionPath), "missing additive v0.6.0 expansion migration");
        assertTrue(Files.isRegularFile(verificationPath), "missing read-only v0.6.0 verification script");
        expansion = read(expansionPath);
        verification = read(verificationPath);
    }

    @Test
    void consolidatedSchemaContainsSkuStockReservationAndAuditFacts() {
        String productSku = tableDefinition(schema, "t_product_sku");
        String skuStock = tableDefinition(schema, "t_sku_stock");
        String reservation = tableDefinition(schema, "t_inventory_reservation");
        String inventoryOperation = tableDefinition(schema, "t_inventory_operation");

        containsAll(productSku,
                "`product_id` bigint", "`sku_code` varchar(64)", "`sku_name` varchar(255)",
                "unique key `uk_product_sku_product` (`product_id`)",
                "unique key `uk_product_sku_code` (`sku_code`)");
        containsAll(skuStock,
                "`sku_id` bigint", "`available_stock` int", "`locked_stock` int",
                "`version` bigint", "check (`available_stock` >= 0)", "check (`locked_stock` >= 0)");
        containsAll(reservation,
                "`reservation_no` varchar(64)", "`order_no` varchar(64)",
                "`order_item_id` bigint", "`sku_id` bigint", "`quantity` int",
                "`status` varchar(32)", "`expires_at` datetime", "`committed_at` datetime",
                "`released_at` datetime", "unique key `uk_reservation_no` (`reservation_no`)",
                "unique key `uk_reservation_order_item` (`order_item_id`)",
                "check (`quantity` > 0)");
        containsAll(inventoryOperation,
                "`sku_id` bigint", "`order_item_id` bigint", "`receipt_request_id` varchar(64)",
                "`available_delta` int", "`locked_delta` int",
                "unique key `uk_inventory_business_key` (`business_key`)");
    }

    @Test
    void orderAndItemContainIndependentMoneyLifecycleAndFulfillmentFacts() {
        String order = tableDefinition(schema, "t_order_info");
        String item = tableDefinition(schema, "t_order_item");

        containsAll(order,
                "`goods_amount` bigint", "`discount_amount` bigint", "`freight_amount` bigint",
                "`payable_amount` bigint", "`paid_amount` bigint", "`refund_frozen_amount` bigint",
                "`refunded_amount` bigint", "`freight_refund_frozen_amount` bigint",
                "`freight_refunded_amount` bigint", "`effective_payment_no` varchar(64)",
                "`pay_status` varchar(32)", "`fulfillment_status` varchar(32)",
                "`refund_status` varchar(32)", "`close_reason` varchar(32)",
                "`expires_at` datetime", "`close_requested_at` datetime", "`paid_at` datetime",
                "unique key `uk_order_effective_payment` (`effective_payment_no`)");
        containsAll(item,
                "`sku_id` bigint", "`product_name_snapshot` varchar(255)",
                "`sku_name_snapshot` varchar(255)", "`sku_spec_snapshot` json",
                "`original_unit_amount` bigint", "`deal_unit_amount` bigint",
                "`original_total_amount` bigint", "`discount_amount` bigint", "`pay_amount` bigint",
                "`refund_frozen_qty` int", "`refunded_qty` int",
                "`refund_frozen_amount` bigint", "`refunded_amount` bigint",
                "`shipped_qty` int", "`cancel_confirmed_qty` int", "`received_qty` int",
                "`restocked_qty` int", "check (`quantity` > 0)");
    }

    @Test
    void paymentAttemptsPermitManyAttemptsButKeepEveryChannelFactUnique() {
        String paymentOrder = tableDefinition(schema, "t_payment_order");
        String paymentInfo = tableDefinition(schema, "t_payment_info");

        containsAll(paymentOrder,
                "`payment_no` varchar(64)", "`order_no` varchar(64)", "`payment_app_id` bigint",
                "`channel` varchar(32)", "`protocol` varchar(32)",
                "`channel_merchant_order_no` varchar(128)", "`channel_transaction_id` varchar(128)",
                "`request_amount` bigint", "`paid_amount` bigint", "`payer_cash_amount` bigint",
                "`receipt_amount` bigint", "`currency` varchar(8)", "`status` varchar(32)",
                "`disposition` varchar(32)", "`request_id` varchar(64)",
                "`request_fingerprint` char(64)", "`payment_params` text",
                "`refund_frozen_amount` bigint", "`refunded_amount` bigint",
                "unique key `uk_payment_no` (`payment_no`)",
                "unique key `uk_payment_request` (`order_no`, `request_id`)",
                "unique key `uk_payment_merchant_no` (`payment_app_id`, `channel_merchant_order_no`)",
                "unique key `uk_payment_transaction` (`payment_app_id`, `channel_transaction_id`)");
        containsAll(paymentInfo,
                "`payment_no` varchar(64)", "`payment_app_id` bigint",
                "unique key `uk_payment_info_payment_no` (`payment_no`)",
                "unique key `uk_payment_info_transaction` (`payment_app_id`, `transaction_id`)");
        assertFalse(paymentInfo.contains("uk_order_payment_type"),
                "one payment type per order prevents legitimate multiple payment attempts");
    }

    @Test
    void refundsReceiptsAndFulfillmentKeepMoneyAndPhysicalEvidenceSeparate() {
        String refund = tableDefinition(schema, "t_refund_info");
        String refundItem = tableDefinition(schema, "t_refund_item");
        String receipt = tableDefinition(schema, "t_return_receipt");
        String fulfillment = tableDefinition(schema, "t_fulfillment_operation");

        containsAll(refund,
                "`payment_no` varchar(64)", "`refund_source` varchar(16)",
                "`refund_type` varchar(32)", "`request_fingerprint` char(64)",
                "`goods_refund_amount` bigint", "`freight_refund_amount` bigint",
                "`refund_amount` bigint", "`channel_refund_id` varchar(128)",
                "`channel_status` varchar(32)", "`system_refund_key` varchar(160)",
                "unique key `uk_refund_system_key` (`system_refund_key`)");
        assertFalse(refund.contains("unique key `uk_refund_payment_type` (`payment_no`, `refund_type`)"),
                "a global payment/type key would block valid repeated user partial refunds");
        containsAll(refundItem,
                "`sku_id` bigint", "`quota_kind` varchar(16)", "`quantity` int",
                "`refund_amount` bigint", "`received_qty` int", "`accepted_qty` int",
                "`restock_authorized_qty` int", "`restocked_qty` int",
                "unique key `uk_refund_item` (`refund_id`, `order_item_id`)");
        assertFalse(refundItem.contains("restocked_qty` <= `refunded_qty"),
                "physical restock must not depend on channel refund timing");
        containsAll(receipt,
                "`refund_id` bigint", "`refund_item_id` bigint",
                "`receipt_request_id` varchar(64)", "`received_qty` int",
                "`saleable_qty` int", "`rejected_qty` int",
                "unique key `uk_return_receipt_request` (`refund_item_id`, `receipt_request_id`)");
        containsAll(fulfillment,
                "`order_no` varchar(64)", "`order_item_id` bigint", "`request_id` varchar(64)",
                "`operation_type` varchar(16)", "`quantity` int",
                "unique key `uk_fulfillment_operation` (`order_no`, `request_id`, `order_item_id`, `operation_type`)");
    }

    @Test
    void migrationIsAdditiveAndVerificationIsReadOnly() {
        assertFalse(expansion.matches("(?s).*\\b(drop|truncate)\\b.*"),
                "expansion migration must not destroy existing data");
        containsAll(expansion,
                "create table if not exists `t_product_sku`",
                "create table if not exists `t_sku_stock`",
                "create table if not exists `t_payment_order`",
                "create table if not exists `t_inventory_reservation`",
                "create table if not exists `t_return_receipt`",
                "create table if not exists `t_fulfillment_operation`");

        String executableVerification = stripSqlComments(verification);
        assertTrue(executableVerification.contains("select"), "verification script must report migration blockers");
        assertFalse(executableVerification.matches("(?s).*\\b(insert|update|delete|replace|alter|create|drop|truncate)\\b.*"),
                "verification script must be read-only");
    }

    private static String read(Path path) throws Exception {
        return new String(Files.readAllBytes(path), StandardCharsets.UTF_8)
                .replace("\r\n", "\n")
                .toLowerCase();
    }

    private static String tableDefinition(String sql, String tableName) {
        String marker = "create table `" + tableName + "`";
        int start = sql.indexOf(marker);
        assertTrue(start >= 0, "missing table " + tableName);
        int end = sql.indexOf(") engine", start);
        assertTrue(end > start, "unterminated table " + tableName);
        return sql.substring(start, end);
    }

    private static void containsAll(String text, String... fragments) {
        for (String fragment : fragments) {
            assertTrue(text.contains(fragment), "missing SQL contract fragment: " + fragment);
        }
    }

    private static String stripSqlComments(String value) {
        return value.replaceAll("(?m)^\\s*--.*$", "")
                .replaceAll("(?s)/\\*.*?\\*/", "");
    }
}
