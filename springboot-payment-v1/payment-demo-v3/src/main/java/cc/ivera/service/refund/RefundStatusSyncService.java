package cc.ivera.service.refund;

import cc.ivera.service.RefundInfoService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class RefundStatusSyncService {

    private final RefundInfoService refundInfoService;

    public RefundStatusSyncService(RefundInfoService refundInfoService) {
        this.refundInfoService = refundInfoService;
    }

    @Transactional(rollbackFor = Exception.class)
    public boolean syncStatus(RefundStatusSyncResult syncResult) {
        return refundInfoService.syncRefundStatus(syncResult);
    }

    /**
     * 由持锁方调用，确保本地退款数据修复和状态同步都在锁内事务执行。
     */
    @Transactional(rollbackFor = Exception.class)
    public void repairAndSync(RefundStatusSyncResult syncResult) {
        refundInfoService.repairRefundFromChannel(syncResult);
        refundInfoService.syncRefundStatus(syncResult);
    }
}
