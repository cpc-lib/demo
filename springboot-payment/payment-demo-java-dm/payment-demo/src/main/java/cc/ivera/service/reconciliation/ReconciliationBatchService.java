package cc.ivera.service.reconciliation;

import cc.ivera.dto.reconciliation.ReconciliationBatchCreateDTO;
import cc.ivera.dto.reconciliation.ReconciliationBatchQueryDTO;
import cc.ivera.dto.reconciliation.ReconciliationDetailQueryDTO;
import cc.ivera.dto.reconciliation.ReconciliationDiscrepancyQueryDTO;
import cc.ivera.dto.reconciliation.ReconciliationDiscrepancyResolveDTO;
import cc.ivera.entity.reconciliation.ReconciliationBatch;
import cc.ivera.entity.reconciliation.ReconciliationDetail;
import cc.ivera.entity.reconciliation.ReconciliationDiscrepancy;
import cc.ivera.vo.reconciliation.ReconciliationBatchVO;
import cc.ivera.vo.reconciliation.ReconciliationProgressVO;
import cc.ivera.vo.reconciliation.ReconciliationSummaryVO;
import com.baomidou.mybatisplus.core.metadata.IPage;

import java.util.List;

public interface ReconciliationBatchService {

    ReconciliationBatchVO createBatch(ReconciliationBatchCreateDTO dto);

    ReconciliationBatchVO getBatchByNo(String batchNo);

    IPage<ReconciliationBatchVO> pageBatches(ReconciliationBatchQueryDTO dto);

    void asyncExecuteBatch(String batchNo);

    void executeBatch(String batchNo);

    ReconciliationProgressVO getProgress(String batchNo);

    ReconciliationSummaryVO getSummary();

    void resolveDiscrepancy(Long discrepancyId, ReconciliationDiscrepancyResolveDTO dto);

    IPage<ReconciliationDetail> pageDetails(ReconciliationDetailQueryDTO dto);

    IPage<ReconciliationDiscrepancy> pageDiscrepancies(ReconciliationDiscrepancyQueryDTO dto);

    @Deprecated
    ReconciliationBatch createBatch(String channelCode, Long paymentAppId, String billDate);

    @Deprecated
    ReconciliationBatch getByBatchNo(String batchNo);

    @Deprecated
    List<ReconciliationBatch> listBatches(String channelCode, String status, String billDate);

    @Deprecated
    void resolveDiscrepancy(Long discrepancyId, String resolveRemark);

    @Deprecated
    List<ReconciliationDetail> listDetails(String batchNo, String matchStatus);

    @Deprecated
    List<ReconciliationDiscrepancy> listDiscrepancies(String batchNo, String status);
}
