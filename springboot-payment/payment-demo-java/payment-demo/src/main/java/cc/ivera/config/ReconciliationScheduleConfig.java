package cc.ivera.config;

import cc.ivera.dto.ChannelBillImportRequest;
import cc.ivera.dto.ReconciliationExecuteRequest;
import cc.ivera.service.reconciliation.ChannelBillService;
import cc.ivera.service.reconciliation.ReconciliationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

/**
 * 渠道账单自动导入 + 自动对账定时任务
 *
 * 渠道账单为 T+1 出账（微信昨日账单次日 10:00 后生成），
 * 因此默认在账单生成之后执行：先导入账单作为对账依据，再执行对账。
 */
@Slf4j
@Component
public class ReconciliationScheduleConfig {

    private final ReconciliationService reconciliationService;
    private final ChannelBillService channelBillService;

    @Value("${payment.reconciliation.enabled:true}")
    private boolean enabled;

    public ReconciliationScheduleConfig(ReconciliationService reconciliationService,
                                        ChannelBillService channelBillService) {
        this.reconciliationService = reconciliationService;
        this.channelBillService = channelBillService;
    }

    /**
     * 微信：昨日账单次日 10:00 后生成，默认 10:30 拉取导入并对账
     */
    @Scheduled(cron = "${payment.reconciliation.wx-cron:0 30 10 * * ?}")
    public void autoImportAndReconcileWxPay() {
        autoImportAndReconcile("WXPAY", "ALL");
    }

    /**
     * 支付宝：昨日账单次日生成，默认 11:00 拉取导入并对账
     */
    @Scheduled(cron = "${payment.reconciliation.ali-cron:0 0 11 * * ?}")
    public void autoImportAndReconcileAliPay() {
        autoImportAndReconcile("ALIPAY", null);
    }

    private void autoImportAndReconcile(String channelCode, String billType) {
        if (!enabled) {
            log.info("对账定时任务已关闭，跳过执行，channelCode={}", channelCode);
            return;
        }
        LocalDate yesterday = LocalDate.now().minusDays(1);

        // 1. 先导入渠道账单（账单是对账依据；未生成/拉取失败则跳过本次对账）
        try {
            ChannelBillImportRequest importRequest = new ChannelBillImportRequest();
            importRequest.setBillDate(yesterday.toString());
            importRequest.setChannelCode(channelCode);
            importRequest.setBillType(billType);
            channelBillService.importFromChannel(importRequest);
            log.info("自动导入{}账单完成，日期={}", channelCode, yesterday);
        } catch (Exception e) {
            log.error("自动导入{}账单失败，跳过本次对账，日期={}", channelCode, yesterday, e);
            return;
        }

        // 2. 基于已导入账单执行对账
        try {
            ReconciliationExecuteRequest request = new ReconciliationExecuteRequest();
            request.setBillDate(yesterday.toString());
            request.setChannelCode(channelCode);
            request.setBillType(billType);
            reconciliationService.executeReconciliation(request);
            log.info("{}自动对账完成，日期={}", channelCode, yesterday);
        } catch (Exception e) {
            log.error("{}自动对账失败，日期={}", channelCode, yesterday, e);
        }
    }
}
