package cc.ivera.service.impl.reconciliation;

import cc.ivera.exception.BizException;
import lombok.extern.slf4j.Slf4j;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.ss.usermodel.Workbook;
import org.apache.poi.ss.usermodel.WorkbookFactory;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.StringReader;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class WxBillParser {

    private static final String TRADE_SUCCESS = "SUCCESS";

    private static final String TRADE_REFUND = "REFUND";

    private static final String BUSINESS_PAYMENT = "PAYMENT";

    private static final String BUSINESS_REFUND = "REFUND";

    private static final String[] OFFICIAL_ALL_HEADERS = {
            "交易时间", "公众账号ID", "商户号", "特约商户号", "设备号", "微信订单号", "商户订单号",
            "用户标识", "交易类型", "交易状态", "付款银行", "货币种类", "应结订单金额", "代金券金额",
            "微信退款单号", "商户退款单号", "退款金额", "充值券退款金额", "退款类型", "退款状态",
            "商品名称", "商户数据包", "手续费", "费率", "订单金额", "申请退款金额", "费率备注"
    };

    /**
     * 将手动上传的微信文本或 XLSX 统一为可重复解析的文本格式。
     */
    public String normalize(byte[] bytes, String fileName) {
        if (bytes == null || bytes.length == 0) {
            return "";
        }
        if (isXlsx(bytes, fileName)) {
            return normalizeWorkbook(bytes);
        }
        String content = new String(bytes, StandardCharsets.UTF_8);
        return content.startsWith("\uFEFF") ? content.substring(1) : content;
    }

    public List<ChannelBillRecord> parse(String billContent) {
        List<ChannelBillRecord> records = new ArrayList<>();
        if (billContent == null || billContent.trim().isEmpty()) {
            return records;
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(billContent))) {
            String line;
            String[] headers = null;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
            sdf.setLenient(false);

            while ((line = reader.readLine()) != null) {
                if (line.trim().isEmpty()) {
                    continue;
                }

                String[] fields = parseDelimitedLine(line);
                String firstField = fields.length == 0 ? "" : normalizeField(fields[0]);

                if ("交易时间".equals(firstField)) {
                    headers = normalizeFields(fields);
                    continue;
                }

                if (isSummaryRow(firstField)) {
                    continue;
                }

                if (headers == null && isOfficialAllDataRow(fields)) {
                    headers = OFFICIAL_ALL_HEADERS;
                }

                if (headers != null && looksLikeTradeTime(firstField) && fields.length >= headers.length) {
                    try {
                        ChannelBillRecord record = buildRecord(headers, normalizeFields(fields), sdf);
                        if (record != null) {
                            records.add(record);
                        }
                    } catch (Exception e) {
                        log.warn("解析微信账单行失败: {}", line, e);
                    }
                }
            }
        } catch (Exception e) {
            throw new BizException("解析微信账单失败", e);
        }

        long paymentCount = records.stream()
                .filter(record -> BUSINESS_PAYMENT.equals(record.getBusinessType()))
                .count();
        long refundCount = records.stream()
                .filter(record -> BUSINESS_REFUND.equals(record.getBusinessType()))
                .count();
        log.info("微信账单解析完成，共{}条记录，进账{}条，退款{}条",
                records.size(), paymentCount, refundCount);
        return records;
    }

    private ChannelBillRecord buildRecord(String[] headers, String[] fields, SimpleDateFormat sdf) throws ParseException {
        ChannelBillRecord record = new ChannelBillRecord();
        Integer settledAmount = null;
        Integer orderAmount = null;
        Integer refundAmount = null;
        String tradeStatus = null;
        String refundStatus = null;

        for (int i = 0; i < headers.length; i++) {
            String header = normalizeField(headers[i]);
            String value = i < fields.length ? normalizeField(fields[i]) : "";

            switch (header) {
                case "交易时间":
                    if (!value.isEmpty()) {
                        record.setTradeTime(sdf.parse(value));
                    }
                    break;
                case "微信订单号":
                    record.setTransactionId(value);
                    break;
                case "商户订单号":
                    record.setOrderNo(value);
                    break;
                case "交易类型":
                    record.setTradeType(value);
                    break;
                case "交易状态":
                    tradeStatus = value;
                    break;
                case "订单金额":
                    if (!value.isEmpty()) {
                        orderAmount = yuanToFen(value);
                    }
                    break;
                case "应结订单金额":
                    if (!value.isEmpty()) {
                        settledAmount = yuanToFen(value);
                    }
                    break;
                case "退款金额":
                    if (!value.isEmpty()) {
                        refundAmount = yuanToFen(value);
                    }
                    break;
                case "申请退款金额":
                    if (!value.isEmpty()) {
                        refundAmount = yuanToFen(value);
                    }
                    break;
                case "微信退款单号":
                    record.setRefundId(normalizeRefundIdentifier(value));
                    break;
                case "商户退款单号":
                    record.setRefundNo(normalizeRefundIdentifier(value));
                    break;
                case "退款状态":
                    refundStatus = value;
                    break;
                default:
                    break;
            }
        }

        if ((record.getOrderNo() == null || record.getOrderNo().trim().isEmpty())
                && (record.getTransactionId() == null || record.getTransactionId().trim().isEmpty())) {
            return null;
        }

        boolean isRefund = TRADE_REFUND.equalsIgnoreCase(tradeStatus)
                || hasText(record.getRefundNo()) || hasText(record.getRefundId());
        if (isRefund) {
            record.setBusinessType(BUSINESS_REFUND);
            record.setRefundAmount(refundAmount);
            record.setAmount(refundAmount);
            record.setStatus(hasText(refundStatus) ? refundStatus : TRADE_REFUND);
            return record;
        }

        if (!TRADE_SUCCESS.equalsIgnoreCase(tradeStatus)) {
            return null;
        }
        record.setBusinessType(BUSINESS_PAYMENT);
        record.setAmount(orderAmount != null ? orderAmount : settledAmount);
        record.setStatus(tradeStatus);
        return record;
    }

    private String[] parseDelimitedLine(String line) {
        if (line.indexOf('\t') >= 0) {
            return line.split("\\t", -1);
        }
        return parseCsvLine(line);
    }

    private String[] parseCsvLine(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder current = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);
            if (c == '"') {
                inQuotes = !inQuotes;
            } else if (c == ',' && !inQuotes) {
                fields.add(current.toString().trim());
                current = new StringBuilder();
            } else {
                current.append(c);
            }
        }
        fields.add(current.toString().trim());
        return fields.toArray(new String[0]);
    }

    private String[] normalizeFields(String[] fields) {
        String[] normalized = new String[fields.length];
        for (int i = 0; i < fields.length; i++) {
            normalized[i] = normalizeField(fields[i]);
        }
        return normalized;
    }

    private String normalizeField(String value) {
        if (value == null) {
            return "";
        }
        String normalized = value.trim();
        if (normalized.startsWith("\uFEFF")) {
            normalized = normalized.substring(1);
        }
        if (normalized.startsWith("`")) {
            normalized = normalized.substring(1);
        }
        return normalized.trim();
    }

    private boolean isOfficialAllDataRow(String[] fields) {
        return fields.length == OFFICIAL_ALL_HEADERS.length
                && looksLikeTradeTime(normalizeField(fields[0]));
    }

    private boolean looksLikeTradeTime(String value) {
        return value != null && value.matches("\\d{4}-\\d{2}-\\d{2} \\d{2}:\\d{2}:\\d{2}");
    }

    private boolean isSummaryRow(String firstField) {
        return firstField.startsWith("总交易单数")
                || firstField.startsWith("应结订单总金额")
                || firstField.startsWith("订单总金额")
                || firstField.startsWith("总退款金额");
    }

    private boolean hasText(String value) {
        return value != null && !value.trim().isEmpty();
    }

    private String normalizeRefundIdentifier(String value) {
        return "0".equals(value) ? "" : value;
    }

    private Integer yuanToFen(String yuan) {
        try {
            BigDecimal yuanDecimal = new BigDecimal(yuan);
            return yuanDecimal.multiply(new BigDecimal("100")).intValue();
        } catch (Exception e) {
            return 0;
        }
    }

    private boolean isXlsx(byte[] bytes, String fileName) {
        boolean xlsxName = fileName != null && fileName.toLowerCase().endsWith(".xlsx");
        boolean zipSignature = bytes.length >= 4
                && bytes[0] == 'P' && bytes[1] == 'K'
                && bytes[2] == 3 && bytes[3] == 4;
        return xlsxName || zipSignature;
    }

    private String normalizeWorkbook(byte[] bytes) {
        try (Workbook workbook = WorkbookFactory.create(new ByteArrayInputStream(bytes))) {
            if (workbook.getNumberOfSheets() == 0) {
                return "";
            }
            Sheet sheet = workbook.getSheetAt(0);
            DataFormatter formatter = new DataFormatter();
            StringBuilder content = new StringBuilder();

            for (int rowIndex = sheet.getFirstRowNum(); rowIndex <= sheet.getLastRowNum(); rowIndex++) {
                Row row = sheet.getRow(rowIndex);
                if (row == null || row.getLastCellNum() < 0) {
                    continue;
                }
                for (int cellIndex = 0; cellIndex < row.getLastCellNum(); cellIndex++) {
                    if (cellIndex > 0) {
                        content.append('\t');
                    }
                    Cell cell = row.getCell(cellIndex, Row.MissingCellPolicy.RETURN_BLANK_AS_NULL);
                    if (cell != null) {
                        content.append(formatter.formatCellValue(cell)
                                .replace('\r', ' ')
                                .replace('\n', ' '));
                    }
                }
                content.append('\n');
            }
            return content.toString();
        } catch (Exception e) {
            throw new BizException("读取微信Excel账单失败，请确认文件为有效的xlsx格式", e);
        }
    }
}
