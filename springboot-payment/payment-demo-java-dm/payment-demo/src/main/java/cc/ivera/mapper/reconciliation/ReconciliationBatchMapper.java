package cc.ivera.mapper.reconciliation;

import cc.ivera.dto.reconciliation.ReconciliationBatchQueryDTO;
import cc.ivera.entity.reconciliation.ReconciliationBatch;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReconciliationBatchMapper extends BaseMapper<ReconciliationBatch> {

    ReconciliationBatch selectByBatchNo(@Param("batchNo") String batchNo);

    List<ReconciliationBatch> selectByChannelAndDate(@Param("channelCode") String channelCode, @Param("billDate") String billDate);

    List<ReconciliationBatch> selectByStatus(@Param("status") String status);

    IPage<ReconciliationBatch> selectPageByCondition(Page<ReconciliationBatch> page, @Param("query") ReconciliationBatchQueryDTO query);
}
