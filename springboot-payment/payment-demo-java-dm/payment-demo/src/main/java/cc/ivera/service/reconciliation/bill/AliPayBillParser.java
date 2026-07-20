package cc.ivera.service.reconciliation.bill;

import cc.ivera.entity.reconciliation.ReconciliationDetail;
import lombok.extern.slf4j.Slf4j;

import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.List;

@Slf4j
public class AliPayBillParser {

    private static final String HEADER_KEYWORD = "交易创建时间";

    private static final String SUMMARY_KEYWORD = "合计";

    private static final SimpleDateFormat DATE_FORMAT = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

    public static List<ReconciliationDetail> parseTradeBillCsv(String csvContent, String batchNo) {
        List<ReconciliationDetail> result = new ArrayList<>();
        if (csvContent == null || csvContent.trim().isEmpty()) {
            return result;
        }

        String[] lines = csvContent.split("\\r?\\n");
        boolean headerFound = false;

        for (String line : lines) {
            if (line == null || line.trim().isEmpty()) {
                continue;
            }

            String trimmed = line.trim();

            if (!headerFound) {
                if (trimmed.contains(HEADER_KEYWORD)) {
                    headerFound = true;
                }
                continue;
            }

            if (trimmed.contains(SUMMARY_KEYWORD)) {
                continue;
            }

            try {
                ReconciliationDetail detail = parseLine(trimmed, batchNo);
                if (detail != null) {
                    result.add(detail);
                }
            } catch (Exception e) {
                log.warn("解析支付宝账单行失败，跳过该行：{}，原因：{}", trimmed, e.getMessage());
            }
        }

        log.info("支付宝账单解析完成，共解析 {} 条交易记录，批次号：{}", result.size(), batchNo);
        return result;
    }

    private static ReconciliationDetail parseLine(String line, String batchNo) throws ParseException {
        List<String> fields = parseCsvFields(line);
        if (fields.size() < 12) {
            return null;
        }

        ReconciliationDetail detail = new ReconciliationDetail();
        detail.setBatchNo(batchNo);
        detail.setOrderNo(removeQuotes(fields.get(4)));
        detail.setTransactionId(removeQuotes(fields.get(3)));
        detail.setChannelStatus(removeQuotes(fields.get(8)));
        detail.setChannelAmount(yuanToCents(removeQuotes(fields.get(10))));
        detail.setTradeTime(DATE_FORMAT.parse(removeQuotes(fields.get(0))));
        detail.setTradeType("支付");

        return detail;
    }

    private static List<String> parseCsvFields(String line) {
        List<String> fields = new ArrayList<>();
        StringBuilder currentField = new StringBuilder();
        boolean inQuotes = false;

        for (int i = 0; i < line.length(); i++) {
            char c = line.charAt(i);

            if (inQuotes) {
                if (c == '"') {
                    if (i + 1 < line.length() && line.charAt(i + 1) == '"') {
                        currentField.append('"');
                        i++;
                    } else {
                        inQuotes = false;
                    }
                } else {
                    currentField.append(c);
                }
            } else {
                if (c == '"') {
                    inQuotes = true;
                } else if (c == ',') {
                    fields.add(currentField.toString());
                    currentField = new StringBuilder();
                } else {
                    currentField.append(c);
                }
            }
        }
        fields.add(currentField.toString());

        return fields;
    }

    private static String removeQuotes(String value) {
        if (value == null) {
            return null;
        }
        String trimmed = value.trim();
        if (trimmed.startsWith("\"") && trimmed.endsWith("\"") && trimmed.length() >= 2) {
            return trimmed.substring(1, trimmed.length() - 1);
        }
        return trimmed;
    }

    private static Integer yuanToCents(String yuan) {
        if (yuan == null || yuan.trim().isEmpty()) {
            return null;
        }
        try {
            return new BigDecimal(yuan.trim()).movePointRight(2).intValueExact();
        } catch (Exception e) {
            log.warn("金额转换失败：{}", yuan);
            return null;
        }
    }
}
