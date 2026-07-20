package cc.ivera.job;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.dto.reconciliation.ReconciliationBatchCreateDTO;
import cc.ivera.mq.ReconciliationExecuteProducer;
import cc.ivera.service.reconciliation.ReconciliationBatchService;
import cc.ivera.vo.reconciliation.ReconciliationBatchVO;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Component
@EnableScheduling
@ConditionalOnProperty(name = "payment.reconciliation.schedule.enabled", havingValue = "true", matchIfMissing = false)
@Slf4j
public class ReconciliationScheduler {

    private final ReconciliationBatchService reconciliationBatchService;

    private final PaymentConfigLoader paymentConfigLoader;

    private final ReconciliationExecuteProducer executeProducer;

    public ReconciliationScheduler(
            ReconciliationBatchService reconciliationBatchService,
            PaymentConfigLoader paymentConfigLoader,
            ReconciliationExecuteProducer executeProducer
    ) {
        this.reconciliationBatchService = reconciliationBatchService;
        this.paymentConfigLoader = paymentConfigLoader;
        this.executeProducer = executeProducer;
    }

    @Scheduled(cron = "${payment.reconciliation.schedule.cron:0 30 10 * * ?}")
    public void dailyReconciliation() {
        log.info("开始执行每日对账任务");
        String billDate = LocalDate.now().minusDays(1).format(DateTimeFormatter.ofPattern("yyyy-MM-dd"));
        log.info("对账日期：{}", billDate);

        String[] channels = {PaymentConfigLoader.CHANNEL_WXPAY, PaymentConfigLoader.CHANNEL_ALIPAY};
        for (String channelCode : channels) {
            List<PaymentAppConfig> appConfigs = paymentConfigLoader.listAppConfigsByChannelCode(channelCode);
            if (appConfigs == null || appConfigs.isEmpty()) {
                log.warn("渠道 {} 没有启用的支付应用，跳过对账", channelCode);
                continue;
            }

            for (PaymentAppConfig appConfig : appConfigs) {
                try {
                    log.info("开始创建对账批次，channelCode={}, appId={}, billDate={}",
                            channelCode, appConfig.getAppId(), billDate);
                    ReconciliationBatchCreateDTO dto = new ReconciliationBatchCreateDTO();
                    dto.setChannelCode(channelCode);
                    dto.setPaymentAppId(appConfig.getAppId());
                    dto.setBillDate(billDate);
                    ReconciliationBatchVO batch = reconciliationBatchService.createBatch(dto);
                    if (batch != null) {
                        log.info("对账批次创建成功，batchNo={}", batch.getBatchNo());
                        executeProducer.sendExecute(batch.getBatchNo());
                    }
                } catch (Exception e) {
                    log.error("创建对账批次失败，channelCode={}, appId={}, billDate={}",
                            channelCode, appConfig.getAppId(), billDate, e);
                }
            }
        }
        log.info("每日对账任务执行完成");
    }
}
