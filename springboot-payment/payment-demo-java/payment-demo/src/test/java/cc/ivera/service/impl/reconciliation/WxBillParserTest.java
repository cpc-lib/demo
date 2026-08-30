package cc.ivera.service.impl.reconciliation;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@DisplayName("微信账单解析器测试")
class WxBillParserTest {

    private final WxBillParser parser = new WxBillParser();

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
    @DisplayName("【现状】解析微信交易账单CSV - 成功订单")
    void testParseSuccessTradeBill() {
        String csv = "\uFEFF账单时间：2026年07月19日 10:00:00\n" +
                "交易时间,公众账号ID,商户号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率\n" +
                "2026-07-19 10:30:00,wx_appid,1234567890,4200001234202607191234567890,ORDER_TEST_001,user_openid,NATIVE,SUCCESS,OTHERS,CNY,1.00,0.00,,,,,,,,测试商品,0.010,0.1%\n" +
                "2026-07-19 11:20:00,wx_appid,1234567890,4200001234202607190987654321,ORDER_TEST_002,user_openid2,NATIVE,SUCCESS,OTHERS,CNY,2.50,0.00,,,,,,,,测试商品2,0.025,0.1%\n" +
                "总交易单数,2\n" +
                "应结订单总金额,3.50\n";

        List<ChannelBillRecord> result = parser.parse(csv);

        assertEquals(2, result.size(), "应该解析出2条记录");

        ChannelBillRecord record1 = result.get(0);
        assertEquals("ORDER_TEST_001", record1.getOrderNo());
        assertEquals("4200001234202607191234567890", record1.getTransactionId());
        assertEquals(100, record1.getAmount());
        assertEquals("SUCCESS", record1.getStatus());
        assertEquals("NATIVE", record1.getTradeType());
        assertNotNull(record1.getTradeTime());

        ChannelBillRecord record2 = result.get(1);
        assertEquals("ORDER_TEST_002", record2.getOrderNo());
        assertEquals(250, record2.getAmount());
    }

    @Test
    @DisplayName("【现状】解析包含退款的微信账单")
    void testParseBillWithRefund() {
        String csv = "\uFEFF账单时间：2026年07月19日\n" +
                "交易时间,公众账号ID,商户号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率\n" +
                "2026-07-19 10:30:00,wx_appid,1234567890,4200001234202607191234567890,ORDER_REFUND_001,user_openid,NATIVE,SUCCESS,OTHERS,CNY,1.00,0.00,5000012345678901234,REFUND_TEST_001,1.00,0.00,ORIGINAL,SUCCESS,测试商品,,0.010,0.1%\n";

        List<ChannelBillRecord> result = parser.parse(csv);

        assertEquals(1, result.size(), "应该解析出1条记录");
        ChannelBillRecord record = result.get(0);
        assertEquals("ORDER_REFUND_001", record.getOrderNo());
        assertEquals(100, record.getAmount());
        assertEquals(100, record.getRefundAmount());
        assertEquals("5000012345678901234", record.getRefundId());
    }

    @Test
    @DisplayName("【现状】金额解析：元转分正确")
    void testAmountYuanToFen() {
        String csv = "\uFEFF账单时间：2026年07月19日\n" +
                "交易时间,公众账号ID,商户号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率\n" +
                "2026-07-19 10:00:00,wx,123,wx123,ORDER_AMOUNT_001,user,NATIVE,SUCCESS,BANK,CNY,0.01,0.00,,,,,,,,商品A,0.0001,0.1%\n" +
                "2026-07-19 11:00:00,wx,123,wx456,ORDER_AMOUNT_002,user,NATIVE,SUCCESS,BANK,CNY,12345.67,0.00,,,,,,,,商品B,12.34567,0.1%\n";

        List<ChannelBillRecord> result = parser.parse(csv);

        assertEquals(2, result.size());
        assertEquals(1, result.get(0).getAmount(), "0.01元应该等于1分");
        assertEquals(1234567, result.get(1).getAmount(), "12345.67元应该等于1234567分");
    }

    @Test
    @DisplayName("【现状】无订单号和交易号的行被过滤")
    void testFilterInvalidRows() {
        String csv = "\uFEFF账单时间：2026年07月19日\n" +
                "交易时间,公众账号ID,商户号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率\n" +
                "2026-07-19 10:00:00,wx,123,,,user,NATIVE,SUCCESS,BANK,CNY,1.00,0.00,,,,,,,,商品A,0.01,0.1%\n";

        List<ChannelBillRecord> result = parser.parse(csv);
        assertTrue(result.isEmpty(), "没有订单号和交易号的记录应该被过滤");
    }

    @Test
    @DisplayName("官方ALL账单：带保护反引号的进账和退款分别解析")
    void testParseOfficialAllPaymentAndRefundRows() {
        String content = officialAllHeaders() + "\n"
                + officialPaymentRow("42000000000000000001", "ORDER_OFFICIAL_001") + "\n"
                + officialRefundRow() + "\n"
                + "总交易单数,总交易额,总退款金额,总代金券或立减优惠退款金额,手续费总金额,订单总金额,申请退款总金额\n"
                + "`2,`2.00,`0.40,`0.00,`0.01,`2.00,`0.60\n";

        List<ChannelBillRecord> result = parser.parse(content);

        assertEquals(2, result.size());

        ChannelBillRecord payment = result.get(0);
        assertEquals("PAYMENT", payment.getBusinessType());
        assertEquals("ORDER_OFFICIAL_001", payment.getOrderNo());
        assertEquals("42000000000000000001", payment.getTransactionId());
        assertEquals(100, payment.getAmount(), "进账优先使用订单金额");
        assertEquals("SUCCESS", payment.getStatus());

        ChannelBillRecord refund = result.get(1);
        assertEquals("REFUND", refund.getBusinessType());
        assertEquals("ORDER_REFUND_001", refund.getOrderNo());
        assertEquals("REFUND_OFFICIAL_001", refund.getRefundNo());
        assertEquals("50000000000000000001", refund.getRefundId());
        assertEquals(60, refund.getAmount(), "退款优先使用申请退款金额");
        assertEquals("SUCCESS", refund.getStatus());
    }

    @Test
    @DisplayName("官方ALL账单：无表头Tab明细仍可解析")
    void testParseHeaderlessOfficialTabRow() {
        String tabRow = officialPaymentRow("42000000000000000002", "ORDER_TAB_001")
                .replace(',', '\t');

        List<ChannelBillRecord> result = parser.parse(tabRow);

        assertEquals(1, result.size());
        assertEquals("PAYMENT", result.get(0).getBusinessType());
        assertEquals("ORDER_TAB_001", result.get(0).getOrderNo());
        assertEquals(100, result.get(0).getAmount());
    }

    @Test
    @DisplayName("官方ALL账单：撤销明细不进入进账退款对账")
    void testIgnoreRevokedRow() {
        String revoked = officialPaymentRow("42000000000000000003", "ORDER_REVOKED_001")
                .replace("`SUCCESS", "`REVOKED");

        List<ChannelBillRecord> result = parser.parse(officialAllHeaders() + "\n" + revoked);

        assertTrue(result.isEmpty());
    }

    @Test
    @DisplayName("官方ALL账单：相同微信订单号的不同商户订单均保留")
    void testKeepRowsWithDuplicateTransactionId() {
        String transactionId = "42000000000000000004";
        String content = officialAllHeaders() + "\n"
                + officialPaymentRow(transactionId, "ORDER_DUP_001") + "\n"
                + officialPaymentRow(transactionId, "ORDER_DUP_002") + "\n";

        List<ChannelBillRecord> result = parser.parse(content);

        assertEquals(2, result.size());
        assertEquals("ORDER_DUP_001", result.get(0).getOrderNo());
        assertEquals("ORDER_DUP_002", result.get(1).getOrderNo());
    }

    private String officialAllHeaders() {
        return "交易时间,公众账号ID,商户号,特约商户号,设备号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率,订单金额,申请退款金额,费率备注";
    }

    private String officialPaymentRow(String transactionId, String orderNo) {
        return "`2026-08-29 10:00:00,`wx_appid,`1900000001,`,`device_1,`" + transactionId
                + ",`" + orderNo
                + ",`openid,`JSAPI,`SUCCESS,`CMB,`CNY,`0.99,`0.00,`0,`0,`0.00,`0.00,`,`,`商品,`,`0.00600,`0.60%,`1.00,`0.00,`";
    }

    private String officialRefundRow() {
        return "`2026-08-29 11:00:00,`wx_appid,`1900000001,`,`device_1,`42000000000000000005"
                + ",`ORDER_REFUND_001,`openid,`JSAPI,`REFUND,`CMB,`CNY,`0.40,`0.00"
                + ",`50000000000000000001,`REFUND_OFFICIAL_001,`0.40,`0.00,`ORIGINAL,`SUCCESS"
                + ",`商品,`,`0.00000,`0.00%,`1.00,`0.60,`";
    }
}
