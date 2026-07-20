package cc.ivera.config;

import cc.ivera.dto.ReconciliationExecuteRequest;
import cc.ivera.service.reconciliation.ReconciliationService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;

@Slf4j
@Component
public class ReconciliationScheduleConfig {

    private final ReconciliationService reconciliationService;

    @Value("${payment.reconciliation.enabled:true}")
    private boolean enabled;

    public ReconciliationScheduleConfig(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @Scheduled(cron = "${payment.reconciliation.cron:0 0 2 * * ?}")
    public void autoReconcileWxPay() {
        if (!enabled) {
            log.info("对账定时任务已关闭，跳过执行");
            return;
        }
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info("自动执行微信对账，日期={}", yesterday);

            ReconciliationExecuteRequest request = new ReconciliationExecuteRequest();
            request.setBillDate(yesterday.toString());
            request.setChannelCode("WXPAY");
            request.setBillType("ALL");

            reconciliationService.executeReconciliation(request);
            log.info("微信自动对账完成，日期={}", yesterday);
        } catch (Exception e) {
            log.error("微信自动对账失败", e);
        }
    }

    @Scheduled(cron = "${payment.reconciliation.cron:0 30 2 * * ?}")
    public void autoReconcileAliPay() {
        if (!enabled) {
            return;
        }
        try {
            LocalDate yesterday = LocalDate.now().minusDays(1);
            log.info("自动执行支付宝对账，日期={}", yesterday);

            ReconciliationExecuteRequest request = new ReconciliationExecuteRequest();
            request.setBillDate(yesterday.toString());
            request.setChannelCode("ALIPAY");

            reconciliationService.executeReconciliation(request);
            log.info("支付宝自动对账完成，日期={}", yesterday);
        } catch (Exception e) {
            log.error("支付宝自动对账失败", e);
        }
    }
}
