package cc.ivera.service;

import cc.ivera.entity.RefundInfo;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.List;

public interface RefundInfoService extends IService<RefundInfo> {

    RefundInfo createRefundApplication(String orderNo, Integer refundAmount, String reason);

    void updateRefundToProcessing(String refundNo, String contentReturn);

    void updateRefundToSuccess(String refundNo, String refundId, String content);

    void updateRefundToFailed(String refundNo, String content);

    void updateRefundToAbnormal(String refundNo, String content);

    void updateRefundToClosed(String refundNo, String content);

    List<RefundInfo> getNoRefundOrderByDuration(int minutes);

    int getSuccessRefundAmount(String orderNo);

    int getOccupiedRefundAmount(String orderNo);

    int getReservedRefundAmount(String orderNo);

    RefundInfo getByRefundNo(String refundNo);

    List<RefundInfo> listByOrderNo(String orderNo);

    void markApprovalPassed(String refundNo, String approveRemark);

    void markApprovalRejected(String refundNo, String approveRemark);

    void refreshOrderRefundStatus(String orderNo);
}
