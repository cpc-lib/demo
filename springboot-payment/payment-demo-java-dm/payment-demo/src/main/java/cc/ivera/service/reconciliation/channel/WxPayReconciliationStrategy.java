package cc.ivera.service.reconciliation.channel;

import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.reconciliation.ReconciliationBatch;
import cc.ivera.entity.reconciliation.ReconciliationDetail;
import cc.ivera.service.reconciliation.bill.WxPayBillParser;
import cc.ivera.service.wxpay.WxPayBillFacade;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
public class WxPayReconciliationStrategy implements ChannelReconciliationStrategy {

    private final WxPayBillFacade wxPayBillFacade;

    public WxPayReconciliationStrategy(WxPayBillFacade wxPayBillFacade) {
        this.wxPayBillFacade = wxPayBillFacade;
    }

    @Override
    public String getChannelCode() {
        return PaymentConfigLoader.CHANNEL_WXPAY;
    }

    @Override
    public List<ReconciliationDetail> downloadAndParseBill(ReconciliationBatch batch) {
        log.info("微信支付对账：下载并解析账单，batchNo={}, billDate={}", batch.getBatchNo(), batch.getBillDate());
        String csvContent = wxPayBillFacade.downloadBill(batch.getBillDate(), "tradebill", null, null, null);
        return WxPayBillParser.parseTradeBillCsv(csvContent, batch.getBatchNo());
    }
}
