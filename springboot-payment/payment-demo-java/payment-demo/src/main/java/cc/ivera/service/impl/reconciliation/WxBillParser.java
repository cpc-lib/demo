package cc.ivera.service.impl.reconciliation;

import cc.ivera.exception.BizException;
import cc.ivera.util.MoneyUtils;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.StringReader;
import java.math.BigDecimal;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;

@Slf4j
@Component
public class WxBillParser {

    private static final String TRADE_SUCCESS = "SUCCESS";
    private static final String REFUND_SUCCESS = "SUCCESS";

    public List<ChannelBillRecord> parse(String billContent) {
        List<ChannelBillRecord> records = new ArrayList<>();
        if (billContent == null || billContent.trim().isEmpty()) {
            return records;
        }

        try (BufferedReader reader = new BufferedReader(new StringReader(billContent))) {
            String line;
            boolean inDataSection = false;
            String[] headers = null;
            SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");

            while ((line = reader.readLine()) != null) {
                line = line.trim();
                if (line.isEmpty()) {
                    continue;
                }

                if (line.startsWith("交易时间")) {
                    headers = parseCsvLine(line);
                    inDataSection = true;
                    continue;
                }

                if (line.startsWith("总交易单数") || line.startsWith("`")) {
                    inDataSection = false;
                    continue;
                }

                if (inDataSection && headers != null) {
                    String[] fields = parseCsvLine(line);
                    if (fields.length >= headers.length) {
                        try {
                            ChannelBillRecord record = buildRecord(headers, fields, sdf);
                            if (record != null) {
                                records.add(record);
                            }
                        } catch (Exception e) {
                            log.warn("解析微信账单行失败: {}", line, e);
                        }
                    }
                }
            }
        } catch (Exception e) {
            throw new BizException("解析微信账单失败", e);
        }

        log.info("微信账单解析完成，共{}条记录", records.size());
        return records;
    }

    private ChannelBillRecord buildRecord(String[] headers, String[] fields, SimpleDateFormat sdf) throws ParseException {
        ChannelBillRecord record = new ChannelBillRecord();

        for (int i = 0; i < headers.length; i++) {
            String header = headers[i].trim();
            String value = i < fields.length ? fields[i].trim() : "";

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
                    record.setStatus(value);
                    break;
                case "应结订单金额":
                case "订单金额":
                    if (!value.isEmpty()) {
                        record.setAmount(yuanToFen(value));
                    }
                    break;
                case "退款金额":
                    if (!value.isEmpty()) {
                        record.setRefundAmount(yuanToFen(value));
                    }
                    break;
                case "微信退款单号":
                    record.setRefundId(value);
                    break;
                default:
                    break;
            }
        }

        if ((record.getOrderNo() == null || record.getOrderNo().trim().isEmpty())
                && (record.getTransactionId() == null || record.getTransactionId().trim().isEmpty())) {
            return null;
        }

        return record;
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

    private Integer yuanToFen(String yuan) {
        try {
            BigDecimal yuanDecimal = new BigDecimal(yuan);
            return yuanDecimal.multiply(new BigDecimal("100")).intValue();
        } catch (Exception e) {
            return 0;
        }
    }
}
