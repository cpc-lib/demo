package cc.ivera.mapper;

import cc.ivera.entity.ReconciliationDetail;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface ReconciliationDetailMapper extends BaseMapper<ReconciliationDetail> {

    IPage<ReconciliationDetail> selectPageByReconciliationId(Page<ReconciliationDetail> page,
                                                               @Param("reconciliationId") Long reconciliationId,
                                                               @Param("diffType") String diffType);

    IPage<ReconciliationDetail> selectDiffPage(Page<ReconciliationDetail> page,
                                                @Param("reconciliationId") Long reconciliationId);

    List<ReconciliationDetail> selectAllByReconciliationId(@Param("reconciliationId") Long reconciliationId);

    void batchInsert(@Param("list") List<ReconciliationDetail> list);
}
