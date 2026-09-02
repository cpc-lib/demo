package cc.ivera.schema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryMapperContractTest {

    @Test
    void productBucketMovesUseConditionalNonNegativeUpdates() throws Exception {
        String xml = read("src", "main", "resources", "mapper", "ProductMapper.xml").toLowerCase();

        String reserve = update(xml, "reserveStock");
        assertTrue(reserve.contains("available_stock = available_stock - #{quantity}"));
        assertTrue(reserve.contains("locked_stock = locked_stock + #{quantity}"));
        assertTrue(reserve.contains("status = 'on_shelf'"));
        assertTrue(reserve.contains("available_stock >= #{quantity}"));

        String commit = update(xml, "commitReservedStock");
        assertTrue(commit.contains("locked_stock = locked_stock - #{quantity}"));
        assertTrue(commit.contains("sold_stock = sold_stock + #{quantity}"));
        assertTrue(commit.contains("locked_stock >= #{quantity}"));

        String release = update(xml, "releaseReservedStock");
        assertTrue(release.contains("available_stock = available_stock + #{quantity}"));
        assertTrue(release.contains("locked_stock = locked_stock - #{quantity}"));
        assertTrue(release.contains("locked_stock >= #{quantity}"));

        assertTrue(reserve.contains("version = version + 1"));
        assertTrue(commit.contains("version = version + 1"));
        assertTrue(release.contains("version = version + 1"));
    }

    @Test
    void orderItemsAreLockedAndTransitionedFromTheExpectedState() throws Exception {
        String source = read(
                "src", "main", "java", "cc", "ivera", "mapper", "OrderItemMapper.java"
        ).toLowerCase();

        assertTrue(source.contains("order by product_id, id for update"));
        assertTrue(source.contains("inventory_status = #{targetstatus}"));
        assertTrue(source.contains("inventory_status = #{currentstatus}"));
    }

    private String update(String xml, String id) {
        String marker = "<update id=\"" + id.toLowerCase() + "\">";
        int start = xml.indexOf(marker);
        int end = xml.indexOf("</update>", start);
        assertTrue(start >= 0 && end > start, "missing mapper update " + id);
        return xml.substring(start, end);
    }

    private String read(String first, String... more) throws Exception {
        return new String(Files.readAllBytes(Paths.get(first, more)), StandardCharsets.UTF_8);
    }
}
