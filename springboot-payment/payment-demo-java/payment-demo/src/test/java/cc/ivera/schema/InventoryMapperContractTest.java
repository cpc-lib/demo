package cc.ivera.schema;

import org.junit.jupiter.api.Test;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;

import static org.junit.jupiter.api.Assertions.assertTrue;

class InventoryMapperContractTest {

    @Test
    void skuBucketMovesUseConditionalNonNegativeUpdates() throws Exception {
        String xml = read("src", "main", "resources", "mapper", "SkuStockMapper.xml").toLowerCase();

        String reserve = update(xml, "reserveStock");
        assertTrue(reserve.contains("available_stock = available_stock - #{quantity}"));
        assertTrue(reserve.contains("locked_stock = locked_stock + #{quantity}"));
        assertTrue(reserve.contains("available_stock >= #{quantity}"));

        String commit = update(xml, "commitLockedStock");
        assertTrue(commit.contains("locked_stock = locked_stock - #{quantity}"));
        assertTrue(commit.contains("locked_stock >= #{quantity}"));

        String release = update(xml, "releaseLockedStock");
        assertTrue(release.contains("available_stock = available_stock + #{quantity}"));
        assertTrue(release.contains("locked_stock = locked_stock - #{quantity}"));
        assertTrue(release.contains("locked_stock >= #{quantity}"));

        assertTrue(reserve.contains("version = version + 1"));
        assertTrue(commit.contains("version = version + 1"));
        assertTrue(release.contains("version = version + 1"));
    }

    @Test
    void productMapperCannotMutateRetiredLegacyStockBuckets() throws Exception {
        String xml = read("src", "main", "resources", "mapper", "ProductMapper.xml").toLowerCase();
        String source = read(
                "src", "main", "java", "cc", "ivera", "mapper", "ProductMapper.java"
        ).toLowerCase();

        assertTrue(!xml.contains("id=\"reservestock\""));
        assertTrue(!xml.contains("id=\"commitreservedstock\""));
        assertTrue(!xml.contains("id=\"releasereservedstock\""));
        assertTrue(!xml.contains("id=\"restoresoldstock\""));
        assertTrue(!xml.contains("id=\"updateavailablestock\""));
        assertTrue(!source.contains("int reservestock("));
        assertTrue(!source.contains("int commitreservedstock("));
        assertTrue(!source.contains("int releasereservedstock("));
        assertTrue(!source.contains("int restoresoldstock("));
        assertTrue(!source.contains("int updateavailablestock("));
    }

    @Test
    void productReadsProjectTheDefaultSkuStockInsteadOfLegacyProductBalances() throws Exception {
        String xml = read("src", "main", "resources", "mapper", "ProductMapper.xml").toLowerCase();

        assertTrue(xml.contains("id=\"selectbyidwithdefaultstock\""));
        assertTrue(xml.contains("id=\"selectbyidswithdefaultstock\""));
        assertTrue(xml.contains("id=\"selectallwithdefaultstock\""));
        assertTrue(xml.contains("join t_product_sku"));
        assertTrue(xml.contains("join t_sku_stock"));
        assertTrue(xml.contains("ss.available_stock as available_stock"));
        assertTrue(xml.contains("ss.locked_stock as locked_stock"));
    }

    @Test
    void orderItemsAreLockedAndTransitionedFromTheExpectedState() throws Exception {
        String itemSource = read(
                "src", "main", "java", "cc", "ivera", "mapper", "OrderItemMapper.java"
        ).toLowerCase();
        String reservationXml = read(
                "src", "main", "resources", "mapper", "InventoryReservationMapper.xml"
        ).toLowerCase();

        assertTrue(itemSource.contains("order by product_id, id for update"));
        assertTrue(itemSource.contains("inventory_status = #{targetstatus}"));
        assertTrue(itemSource.contains("inventory_status = #{currentstatus}"));
        assertTrue(reservationXml.contains("order by sku_id, id"));
        assertTrue(reservationXml.contains("status = #{currentstatus}"));
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
