package cc.ivera.service.reconciliation;

import cc.ivera.dto.ReconciliationExecuteRequest;
import cc.ivera.entity.Reconciliation;
import cc.ivera.entity.ReconciliationDetail;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.time.LocalDate;

public interface ReconciliationService {

    Reconciliation executeReconciliation(ReconciliationExecuteRequest request);

    IPage<Reconciliation> listReconciliation(int pageNum, int pageSize,
                                               LocalDate billDateStart, LocalDate billDateEnd,
                                               String channelCode, String status);

    Reconciliation getReconciliationById(Long id);

    IPage<ReconciliationDetail> listDetails(Long reconciliationId, int pageNum, int pageSize, String diffType);

    IPage<ReconciliationDetail> listDiffDetails(Long reconciliationId, int pageNum, int pageSize);

    String exportReconciliation(Long id);
}
