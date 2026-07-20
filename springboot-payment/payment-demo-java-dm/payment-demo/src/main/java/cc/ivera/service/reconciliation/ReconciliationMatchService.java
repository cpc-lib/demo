package cc.ivera.service.reconciliation;

import cc.ivera.entity.reconciliation.ReconciliationDetail;

import java.util.List;

public interface ReconciliationMatchService {

    void executeMatch(String batchNo);

    List<ReconciliationDetail> collectLocalTransactions(String channelCode, Long paymentAppId, String billDate);
}
