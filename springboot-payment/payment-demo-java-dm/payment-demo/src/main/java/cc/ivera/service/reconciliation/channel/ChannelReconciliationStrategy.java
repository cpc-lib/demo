package cc.ivera.service.reconciliation.channel;

import cc.ivera.entity.reconciliation.ReconciliationBatch;
import cc.ivera.entity.reconciliation.ReconciliationDetail;

import java.util.List;

public interface ChannelReconciliationStrategy {

    String getChannelCode();

    List<ReconciliationDetail> downloadAndParseBill(ReconciliationBatch batch);
}
