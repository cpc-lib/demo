package cc.ivera.service.reconciliation.channel;

import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.reconciliation.ReconciliationBatch;
import cc.ivera.entity.reconciliation.ReconciliationDetail;
import cc.ivera.exception.BizException;
import cc.ivera.service.AliPayService;
import cc.ivera.service.reconciliation.bill.AliPayBillParser;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;

@Slf4j
@Component
public class AliPayReconciliationStrategy implements ChannelReconciliationStrategy {

    private final AliPayService aliPayService;

    public AliPayReconciliationStrategy(AliPayService aliPayService) {
        this.aliPayService = aliPayService;
    }

    @Override
    public String getChannelCode() {
        return PaymentConfigLoader.CHANNEL_ALIPAY;
    }

    @Override
    public List<ReconciliationDetail> downloadAndParseBill(ReconciliationBatch batch) {
        log.info("支付宝对账：下载并解析账单，batchNo={}, billDate={}", batch.getBatchNo(), batch.getBillDate());
        String billDownloadUrl = aliPayService.queryBill(batch.getBillDate(), "trade");
        String csvContent = downloadAliPayBillCsv(billDownloadUrl);
        return AliPayBillParser.parseTradeBillCsv(csvContent, batch.getBatchNo());
    }

    private String downloadAliPayBillCsv(String billDownloadUrl) {
        try {
            URL url = new URL(billDownloadUrl);
            HttpURLConnection conn = (HttpURLConnection) url.openConnection();
            conn.setRequestMethod("GET");
            conn.setConnectTimeout(30000);
            conn.setReadTimeout(60000);

            StringBuilder content = new StringBuilder();
            try (BufferedReader reader = new BufferedReader(
                    new InputStreamReader(conn.getInputStream(), "GBK"))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    content.append(line).append("\n");
                }
            }
            return content.toString();
        } catch (Exception e) {
            throw new BizException("下载支付宝账单CSV失败", e);
        }
    }
}
