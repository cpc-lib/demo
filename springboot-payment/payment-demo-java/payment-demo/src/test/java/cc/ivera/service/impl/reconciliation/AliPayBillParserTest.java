package cc.ivera.service.impl.reconciliation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("支付宝账单解析器测试")
class AliPayBillParserTest {

    private final AliPayBillParser parser = new AliPayBillParser();

    @Test
    @DisplayName("【现状】空内容返回空列表")
    void testParseEmptyContent() {
        List<ChannelBillRecord> result = parser.parse("");
        assertTrue(result.isEmpty(), "空内容应该返回空列表");
    }

    @Test
    @DisplayName("【现状】null 内容返回空列表")
    void testParseNullContent() {
        List<ChannelBillRecord> result = parser.parse(null);
        assertTrue(result.isEmpty(), "null内容应该返回空列表");
    }

    @Test
    @DisplayName("【现状】解析支付宝交易账单CSV - 成功订单")
    void testParseSuccessTradeBill() {
        String csv = "\uFEFF账号信息(账号,姓名,状态,查询时间,页码,每页记录数,总记录数)\n" +
                "2088000000000000,测试商家,正常,2026-07-20 10:00:00,1,50,2\n" +
                "\n" +
                "账务明细说明\n" +
                "----------------------账务明细----------------------\n" +
                "收/支,交易创建时间,交易付款时间,交易号,商家订单号,商品名称,金额（元）,交易状态,服务费（元）,成功退款（元）,备注,资金状态\n" +
                "收入,2026-07-19 10:30:00,2026-07-19 10:30:05,202607191234567890,ALI_ORDER_001,测试商品A,1.00,交易成功,0.01,0.00,,已收入\n" +
                "收入,2026-07-19 11:20:00,2026-07-19 11:20:05,202607190987654321,ALI_ORDER_002,测试商品B,2.50,交易成功,0.025,0.00,,已收入\n";

        List<ChannelBillRecord> result = parser.parse(csv);

        assertEquals(2, result.size(), "应该解析出2条记录");

        ChannelBillRecord record1 = result.get(0);
        assertEquals("ALI_ORDER_001", record1.getOrderNo());
        assertEquals("202607191234567890", record1.getTransactionId());
        assertEquals(100, record1.getAmount());
        assertEquals("交易成功", record1.getStatus());
        assertNotNull(record1.getTradeTime());

        ChannelBillRecord record2 = result.get(1);
        assertEquals("ALI_ORDER_002", record2.getOrderNo());
        assertEquals(250, record2.getAmount());
    }

    @Test
    @DisplayName("【现状】金额解析：元转分正确，处理人民币符号")
    void testAmountYuanToFenWithSymbol() {
        String csv = "\uFEFF账号信息\n" +
                "2088000000000000,测试商家\n" +
                "\n" +
                "账务明细说明\n" +
                "----------------------账务明细----------------------\n" +
                "收/支,交易创建时间,交易付款时间,交易号,商家订单号,商品名称,金额（元）,交易状态,服务费（元）,成功退款（元）,备注,资金状态\n" +
                "收入,2026-07-19 10:00:00,2026-07-19 10:00:05,tx1,ALI_AMOUNT_001,商品A,¥0.01,交易成功,0.0001,0.00,,已收入\n" +
                "收入,2026-07-19 11:00:00,2026-07-19 11:00:05,tx2,ALI_AMOUNT_002,商品B,9999.99,交易成功,9.99999,0.00,,已收入\n";

        List<ChannelBillRecord> result = parser.parse(csv);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getAmount(), "¥0.01应该等于1分");
        assertEquals(999999, result.get(1).getAmount(), "9999.99元应该等于999999分");
    }

    @Test
    @DisplayName("【现状】无订单号和交易号的行被过滤")
    void testFilterInvalidRows() {
        String csv = "\uFEFF账号信息\n" +
                "2088000000000000\n" +
                "\n" +
                "账务明细说明\n" +
                "----------------------账务明细----------------------\n" +
                "收/支,交易创建时间,交易付款时间,交易号,商家订单号,商品名称,金额（元）,交易状态,服务费（元）,成功退款（元）,备注,资金状态\n" +
                "收入,2026-07-19 10:00:00,2026-07-19 10:00:05,,,,测试商品,1.00,交易成功,0.01,0.00,,已收入\n";

        List<ChannelBillRecord> result = parser.parse(csv);
        assertTrue(result.isEmpty(), "没有订单号和交易号的记录应该被过滤");
    }

    @Test
    @DisplayName("【现状】金额为0或负数的行被过滤")
    void testFilterZeroAmountRows() {
        String csv = "\uFEFF账号信息\n" +
                "2088000000000000\n" +
                "\n" +
                "账务明细说明\n" +
                "----------------------账务明细----------------------\n" +
                "收/支,交易创建时间,交易付款时间,交易号,商家订单号,商品名称,金额（元）,交易状态,服务费（元）,成功退款（元）,备注,资金状态\n" +
                "收入,2026-07-19 10:00:00,2026-07-19 10:00:05,tx1,ALI_ZERO_001,商品A,0.00,交易成功,0.00,0.00,,已收入\n";

        List<ChannelBillRecord> result = parser.parse(csv);
        assertTrue(result.isEmpty(), "金额为0的记录应该被过滤");
    }
}
