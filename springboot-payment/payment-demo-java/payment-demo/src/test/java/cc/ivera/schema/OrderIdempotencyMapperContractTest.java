package cc.ivera.schema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class OrderIdempotencyMapperContractTest {

    @Test
    void mapperLocksKeysCompletesIssuedOnceAndNeverCleansCompletedRows() throws Exception {
        String xml = new String(
                Files.readAllBytes(Paths.get("src", "main", "resources", "mapper", "OrderIdempotencyMapper.xml")),
                StandardCharsets.UTF_8
        ).toLowerCase();

        assertTrue(xml.contains("where idempotency_key = #{idempotencykey}"));
        assertTrue(xml.contains("for update"));
        assertTrue(xml.contains("and status = 'issued'\n          and order_id is null"));

        int cleanupStart = xml.indexOf("<delete id=\"deleteunusedexpiredbefore\">");
        int cleanupEnd = xml.indexOf("</delete>", cleanupStart);
        assertTrue(cleanupStart >= 0 && cleanupEnd > cleanupStart);
        String cleanup = xml.substring(cleanupStart, cleanupEnd);
        assertTrue(cleanup.contains("status in ('issued', 'expired')"));
        assertFalse(cleanup.contains("completed"));
    }

    @Test
    void directOrderCreationSourceDoesNotQueryAnArbitraryUnpaidOrder() throws Exception {
        String source = new String(
                Files.readAllBytes(Paths.get(
                        "src", "main", "java", "cc", "ivera", "service", "impl", "OrderInfoServiceImpl.java"
                )),
                StandardCharsets.UTF_8
        );

        assertFalse(source.contains("selectNoPayOrderForUpdate"));
    }
}
