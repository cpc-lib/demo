package cc.ivera.mapper.reconciliation;

import cc.ivera.dto.reconciliation.ReconciliationDiscrepancyQueryDTO;
import cc.ivera.entity.reconciliation.ReconciliationDiscrepancy;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReconciliationDiscrepancyMapper extends BaseMapper<ReconciliationDiscrepancy> {

    List<ReconciliationDiscrepancy> selectByBatchNo(@Param("batchNo") String batchNo);

    List<ReconciliationDiscrepancy> selectByBatchNoAndStatus(@Param("batchNo") String batchNo, @Param("status") String status);

    IPage<ReconciliationDiscrepancy> selectPageByCondition(Page<ReconciliationDiscrepancy> page, @Param("query") ReconciliationDiscrepancyQueryDTO query);
}
