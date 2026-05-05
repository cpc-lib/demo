package cc.ivera.service;

import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.RefundStatus;
import com.baomidou.mybatisplus.extension.service.IService;

import java.util.Collection;
import java.util.List;

public interface RefundInfoService extends IService<RefundInfo> {

    RefundInfo createRefundApplication(String orderNo, Integer refundAmount, String reason);

    void updateRefundToProcessing(String refundNo, String contentReturn);

    void updateRefundToSuccess(String refundNo, String refundId, String content);

    void updateRefundToFailed(String refundNo, String content);

    void updateRefundToAbnormal(String refundNo, String content);

    void updateRefundToClosed(String refundNo, String content);

    boolean updateRefundIfStatusIn(String refundNo,
                                   String refundId,
                                   RefundStatus targetStatus,
                                   String contentReturn,
                                   String contentNotify,
                                   Collection<RefundStatus> currentStatuses);

    RefundInfo getByRefundNo(String refundNo);

    List<RefundInfo> listByOrderNo(String orderNo);

    void markApprovalPassed(String refundNo, String approveRemark);

    void markApprovalRejected(String refundNo, String approveRemark);

    void refreshOrderRefundStatus(String orderNo);
}
