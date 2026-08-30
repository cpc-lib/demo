package cc.ivera.mapper;

import cc.ivera.entity.ChannelBill;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDate;

public interface ChannelBillMapper extends BaseMapper<ChannelBill> {

    IPage<ChannelBill> selectPageByConditions(Page<ChannelBill> page,
                                              @Param("billDateStart") LocalDate billDateStart,
                                              @Param("billDateEnd") LocalDate billDateEnd,
                                              @Param("channelCode") String channelCode,
                                              @Param("billSource") String billSource);

    ChannelBill selectByUniqueKey(@Param("billDate") LocalDate billDate,
                                  @Param("channelCode") String channelCode,
                                  @Param("paymentAppId") Long paymentAppId,
                                  @Param("billType") String billType);
}
