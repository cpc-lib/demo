package cc.ivera.mapper;

import cc.ivera.entity.RefundInfo;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.Collection;

public interface RefundInfoMapper extends BaseMapper<RefundInfo> {

    Integer sumRefundAmountByOrderNoAndStatuses(@Param("orderNo") String orderNo,
                                                @Param("statuses") Collection<String> statuses);

    Integer sumRefundAmountByOrderNoAndApprovalStatuses(@Param("orderNo") String orderNo,
                                                        @Param("approvalStatuses") Collection<String> approvalStatuses);
}
