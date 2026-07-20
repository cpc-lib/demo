package cc.ivera.mapper.reconciliation;

import cc.ivera.dto.reconciliation.ReconciliationDetailQueryDTO;
import cc.ivera.entity.reconciliation.ReconciliationDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReconciliationDetailMapper extends BaseMapper<ReconciliationDetail> {

    List<ReconciliationDetail> selectByBatchNo(@Param("batchNo") String batchNo);

    List<ReconciliationDetail> selectByBatchNoAndMatchStatus(@Param("batchNo") String batchNo, @Param("matchStatus") String matchStatus);

    int batchInsert(@Param("list") List<ReconciliationDetail> list);

    int deleteByBatchNo(@Param("batchNo") String batchNo);

    IPage<ReconciliationDetail> selectPageByCondition(Page<ReconciliationDetail> page, @Param("query") ReconciliationDetailQueryDTO query);
}
