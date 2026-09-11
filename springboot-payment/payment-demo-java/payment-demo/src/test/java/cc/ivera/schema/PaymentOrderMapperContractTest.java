package cc.ivera.schema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PaymentOrderMapperContractTest {

    @Test
    void mapperUsesFrozenPaymentIdentityAndConditionalStateTransitions() throws Exception {
        String xml = new String(
                Files.readAllBytes(Paths.get("src", "main", "resources", "mapper", "PaymentOrderMapper.xml")),
                StandardCharsets.UTF_8
        ).toLowerCase().replaceAll("\\s+", " ");

        assertTrue(xml.contains("where order_no = #{orderno} and request_id = #{requestid}"));
        assertTrue(xml.contains("where payment_app_id = #{paymentappid} and channel_merchant_order_no = #{channelmerchantorderno}"));
        assertTrue(xml.contains("where payment_app_id = #{paymentappid} and channel_transaction_id = #{channeltransactionid}"));
        assertTrue(xml.contains("where payment_no = #{paymentno} and status = #{currentstatus}"));
        assertTrue(xml.contains("status in ('creating', 'unknown')"));
        assertFalse(xml.contains("where order_no = #{paymentno}"),
                "a payment number must never silently fall back to an order number");
    }
}
