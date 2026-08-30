package cc.ivera.service.impl.reconciliation;

import cc.ivera.entity.ChannelBill;
import cc.ivera.exception.BizException;
import cc.ivera.mapper.ChannelBillMapper;
import cc.ivera.mapper.ReconciliationMapper;
import cc.ivera.service.AliPayService;
import cc.ivera.service.wxpay.WxPayBillFacade;
import com.baomidou.mybatisplus.core.metadata.IPage;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.mock.web.MockMultipartFile;

import java.io.ByteArrayOutputStream;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.concurrent.atomic.AtomicReference;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("渠道账单导入测试")
class ChannelBillServiceTest {

    private static final ZoneId BILL_ZONE = ZoneId.of("Asia/Shanghai");

    private ChannelBillMapper channelBillMapper;
    private ReconciliationMapper reconciliationMapper;
    private WxPayBillFacade wxPayBillFacade;
    private ChannelBillServiceImpl billService;

    private final AtomicReference<ChannelBill> insertedBill = new AtomicReference<>();

    @BeforeEach
    void setUp() {
        channelBillMapper = mock(ChannelBillMapper.class);
        reconciliationMapper = mock(ReconciliationMapper.class);
        wxPayBillFacade = mock(WxPayBillFacade.class);
        billService = new ChannelBillServiceImpl(
                channelBillMapper, reconciliationMapper, wxPayBillFacade,
                mock(AliPayService.class), new WxBillParser(), new AliPayBillParser());

        insertedBill.set(null);
        when(channelBillMapper.insert(any(ChannelBill.class))).thenAnswer(invocation -> {
            ChannelBill bill = invocation.getArgument(0);
            bill.setId(1L);
            insertedBill.set(bill);
            return 1;
        });
        when(channelBillMapper.selectById(any())).thenAnswer(invocation -> {
            if (invocation.getArgument(0) == null) {
                return null;
            }
            return insertedBill.get();
        });
    }

    private String yesterday() {
        return LocalDate.now(BILL_ZONE).minusDays(1).toString();
    }

    private MockMultipartFile wxBillFile(String csv) {
        return new MockMultipartFile("file", "wx_bill.csv", "text/csv", csv.getBytes());
    }

    private String wxCsv() {
        return "\uFEFF账单时间：2026年08月27日\n" +
                "交易时间,公众账号ID,商户号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率\n" +
                "2026-08-27 10:30:00,wx_appid,1234567890,4200001234202608271234567890,ORDER_BILL_001,user_openid,NATIVE,SUCCESS,OTHERS,CNY,1.00,0.00,,,,,,,,测试商品,0.010,0.1%\n" +
                "2026-08-27 11:20:00,wx_appid,1234567890,4200001234202608270987654321,ORDER_BILL_002,user_openid2,NATIVE,SUCCESS,OTHERS,CNY,2.50,0.00,,,,,,,,测试商品2,0.025,0.1%\n" +
                "总交易单数,2\n" +
                "应结订单总金额,3.50\n";
    }

    @Test
    @DisplayName("【现状】手动上传合法微信账单CSV：导入成功且统计正确")
    void testUploadWxBillSuccess() {
        when(channelBillMapper.selectByUniqueKey(any(), eq("WXPAY"), any(), eq("ALL"))).thenReturn(null);

        ChannelBill bill = billService.uploadBill(wxBillFile(wxCsv()), yesterday(), "WXPAY", null, null);

        assertThat(bill.getRecordCount()).isEqualTo(2);
        assertThat(bill.getTotalAmount()).isEqualTo(350L);
        assertThat(bill.getBillSource()).isEqualTo("MANUAL_UPLOAD");
        assertThat(bill.getStatus()).isEqualTo("IMPORTED");
        assertThat(bill.getFileName()).isEqualTo("wx_bill.csv");
        assertThat(bill.getBillHash()).isNotBlank();
        assertThat(bill.getBillContent()).contains("ORDER_BILL_001");
        assertThat(bill.getBillDate()).isEqualTo(LocalDate.parse(yesterday()));
    }

    @Test
    @DisplayName("【现状】T+1约束：当日账单禁止导入")
    void testUploadTodayBillRejected() {
        String today = LocalDate.now(BILL_ZONE).toString();

        assertThatThrownBy(() -> billService.uploadBill(wxBillFile(wxCsv()), today, "WXPAY", null, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("T+1");
    }

    @Test
    @DisplayName("【现状】手动上传仅支持微信账单：支付宝渠道被拒绝")
    void testUploadAliPayBillRejected() {
        assertThatThrownBy(() -> billService.uploadBill(wxBillFile(wxCsv()), yesterday(), "ALIPAY", null, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("手动上传当前仅支持微信交易账单");
    }

    @Test
    @DisplayName("【现状】空文件被拒绝")
    void testUploadEmptyFileRejected() {
        assertThatThrownBy(() -> billService.uploadBill(wxBillFile(""), yesterday(), "WXPAY", null, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("账单文件不能为空");
    }

    @Test
    @DisplayName("【现状】账单无有效记录（仅汇总行）导入被拒绝")
    void testUploadBillWithoutRecordsRejected() {
        String csv = "\uFEFF账单时间：2026年08月27日\n" +
                "交易时间,公众账号ID,商户号,微信订单号,商户订单号\n" +
                "总交易单数,0\n" +
                "应结订单总金额,0.00\n";

        assertThatThrownBy(() -> billService.uploadBill(wxBillFile(csv), yesterday(), "WXPAY", null, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("账单解析结果为空");
    }

    @Test
    @DisplayName("【现状】同键账单重复导入：默认幂等返回已存在账单")
    void testReImportWithoutForceReturnsExisting() {
        ChannelBill existing = new ChannelBill();
        existing.setId(9L);
        existing.setBillDate(LocalDate.parse(yesterday()));
        existing.setChannelCode("WXPAY");
        existing.setRecordCount(5);
        when(channelBillMapper.selectByUniqueKey(any(), eq("WXPAY"), any(), eq("ALL"))).thenReturn(existing);

        ChannelBill bill = billService.uploadBill(wxBillFile(wxCsv()), yesterday(), "WXPAY", null, null);

        assertThat(bill.getId()).isEqualTo(9L);
        assertThat(bill.getRecordCount()).isEqualTo(5);
        verify(channelBillMapper, never()).insert(any(ChannelBill.class));
    }

    @Test
    @DisplayName("【现状】同键账单重复导入：force=true覆盖更新原文与统计")
    void testReImportWithForceOverwrites() {
        ChannelBill existing = new ChannelBill();
        existing.setId(9L);
        existing.setBillDate(LocalDate.parse(yesterday()));
        existing.setChannelCode("WXPAY");
        existing.setRecordCount(5);
        when(channelBillMapper.selectByUniqueKey(any(), eq("WXPAY"), any(), eq("ALL"))).thenReturn(existing);
        when(channelBillMapper.updateById(any(ChannelBill.class))).thenReturn(1);
        insertedBill.set(existing);

        ChannelBill bill = billService.uploadBill(wxBillFile(wxCsv()), yesterday(), "WXPAY", null, true);

        assertThat(bill.getId()).isEqualTo(9L);
        assertThat(bill.getRecordCount()).isEqualTo(2);
        assertThat(bill.getTotalAmount()).isEqualTo(350L);
        ArgumentCaptor<ChannelBill> captor = ArgumentCaptor.forClass(ChannelBill.class);
        verify(channelBillMapper).updateById(captor.capture());
        assertThat(captor.getValue().getBillContent()).contains("ORDER_BILL_001");
        verify(channelBillMapper, never()).insert(any(ChannelBill.class));
    }

    @Test
    @DisplayName("【现状】自动拉取导入：下载渠道账单并落库")
    void testImportFromChannelSuccess() {
        when(channelBillMapper.selectByUniqueKey(any(), eq("WXPAY"), any(), eq("ALL"))).thenReturn(null);
        when(wxPayBillFacade.downloadBill(eq((Long) null), any(), eq("tradebill"), eq("ALL"), any(), any()))
                .thenReturn(wxCsv());

        ChannelBill bill = billService.importFromChannel(
                newBillRequest(yesterday(), "WXPAY", false));

        assertThat(bill.getBillSource()).isEqualTo("AUTO_DOWNLOAD");
        assertThat(bill.getRecordCount()).isEqualTo(2);
        assertThat(bill.getTotalAmount()).isEqualTo(350L);
    }

    @Test
    @DisplayName("自动拉取指定微信应用时，下载与账单落库使用同一paymentAppId")
    void testImportFromChannelUsesRequestedPaymentApp() {
        when(channelBillMapper.selectByUniqueKey(any(), eq("WXPAY"), eq(77L), eq("ALL"))).thenReturn(null);
        when(wxPayBillFacade.downloadBill(eq(77L), any(), eq("tradebill"), eq("ALL"), any(), any()))
                .thenReturn(wxCsv());
        cc.ivera.dto.ChannelBillImportRequest request = newBillRequest(yesterday(), "WXPAY", false);
        request.setPaymentAppId(77L);

        ChannelBill bill = billService.importFromChannel(request);

        assertThat(bill.getPaymentAppId()).isEqualTo(77L);
        verify(wxPayBillFacade).downloadBill(eq(77L), any(), eq("tradebill"), eq("ALL"), any(), any());
    }

    @Test
    @DisplayName("【现状】自动拉取导入：当日账单被拒绝（T+1）")
    void testImportFromChannelTodayRejected() {
        assertThatThrownBy(() -> billService.importFromChannel(
                newBillRequest(LocalDate.now(BILL_ZONE).toString(), "WXPAY", false)))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("T+1");
    }

    @Test
    @DisplayName("【现状】被对账记录引用的账单禁止删除")
    void testDeleteReferencedBillRejected() {
        ChannelBill existing = new ChannelBill();
        existing.setId(9L);
        when(channelBillMapper.selectById(9L)).thenReturn(existing);
        when(reconciliationMapper.selectCount(any())).thenReturn(2);

        assertThatThrownBy(() -> billService.deleteBill(9L))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已被对账记录引用");
        verify(channelBillMapper, never()).deleteById(9L);
    }

    @Test
    @DisplayName("【现状】未被引用的账单可删除")
    void testDeleteUnreferencedBillSuccess() {
        ChannelBill existing = new ChannelBill();
        existing.setId(9L);
        when(channelBillMapper.selectById(9L)).thenReturn(existing);
        when(reconciliationMapper.selectCount(any())).thenReturn(0);
        when(channelBillMapper.deleteById(9L)).thenReturn(1);

        billService.deleteBill(9L);

        verify(channelBillMapper).deleteById(9L);
    }

    @Test
    @DisplayName("【现状】查看账单解析记录：分页返回解析结果")
    void testListRecordsPagination() {
        ChannelBill existing = new ChannelBill();
        existing.setId(9L);
        existing.setChannelCode("WXPAY");
        existing.setBillContent(wxCsv());
        when(channelBillMapper.selectById(9L)).thenReturn(existing);

        IPage<ChannelBillRecord> page = billService.listRecords(9L, 1, 1);

        assertThat(page.getTotal()).isEqualTo(2);
        assertThat(page.getRecords()).hasSize(1);
        assertThat(page.getRecords().get(0).getOrderNo()).isEqualTo("ORDER_BILL_001");
    }

    @Test
    @DisplayName("商户平台微信XLSX账单：规范化后导入进账与退款")
    void testUploadWxBillXlsxSuccess() throws Exception {
        when(channelBillMapper.selectByUniqueKey(any(), eq("WXPAY"), any(), eq("ALL"))).thenReturn(null);
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "29082026_ALL.xlsx",
                "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet",
                wxXlsx());

        ChannelBill bill = billService.uploadBill(file, yesterday(), "WXPAY", "ALL", false);

        assertThat(bill.getRecordCount()).isEqualTo(2);
        assertThat(bill.getTotalAmount()).isEqualTo(160L);
        assertThat(bill.getBillContent()).contains("ORDER_XLSX_001");
        assertThat(bill.getBillContent()).contains("REFUND_XLSX_001");
        assertThat(bill.getFileName()).isEqualTo("29082026_ALL.xlsx");
    }

    private byte[] wxXlsx() throws Exception {
        String headers = "交易时间,公众账号ID,商户号,特约商户号,设备号,微信订单号,商户订单号,用户标识,交易类型,交易状态,付款银行,货币种类,应结订单金额,代金券金额,微信退款单号,商户退款单号,退款金额,充值券退款金额,退款类型,退款状态,商品名称,商户数据包,手续费,费率,订单金额,申请退款金额,费率备注";
        String payment = "`2026-08-29 10:00:00,`wx_appid,`1900000001,`,`device,`42000000000000000101,`ORDER_XLSX_001,`openid,`JSAPI,`SUCCESS,`CMB,`CNY,`0.99,`0.00,`0,`0,`0.00,`0.00,`,`,`商品,`,`0.00600,`0.60%,`1.00,`0.00,`";
        String refund = "`2026-08-29 11:00:00,`wx_appid,`1900000001,`,`device,`42000000000000000102,`ORDER_XLSX_002,`openid,`JSAPI,`REFUND,`CMB,`CNY,`0.40,`0.00,`50000000000000000101,`REFUND_XLSX_001,`0.40,`0.00,`ORIGINAL,`SUCCESS,`商品,`,`0.00000,`0.00%,`1.00,`0.60,`";

        try (XSSFWorkbook workbook = new XSSFWorkbook();
             ByteArrayOutputStream output = new ByteArrayOutputStream()) {
            Sheet sheet = workbook.createSheet("ALL");
            fillRow(sheet.createRow(0), headers);
            fillRow(sheet.createRow(1), payment);
            fillRow(sheet.createRow(2), refund);
            fillRow(sheet.createRow(3), "总交易单数,总交易额,总退款金额,总代金券或立减优惠退款金额,手续费总金额,订单总金额,申请退款总金额");
            fillRow(sheet.createRow(4), "`2,`2.00,`0.40,`0.00,`0.01,`2.00,`0.60");
            workbook.write(output);
            return output.toByteArray();
        }
    }

    private void fillRow(Row row, String csvLine) {
        String[] values = csvLine.split(",", -1);
        for (int i = 0; i < values.length; i++) {
            row.createCell(i).setCellValue(values[i]);
        }
    }

    private cc.ivera.dto.ChannelBillImportRequest newBillRequest(String billDate, String channelCode, boolean force) {
        cc.ivera.dto.ChannelBillImportRequest request = new cc.ivera.dto.ChannelBillImportRequest();
        request.setBillDate(billDate);
        request.setChannelCode(channelCode);
        request.setForce(force);
        return request;
    }
}
