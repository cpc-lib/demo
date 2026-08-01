package cc.ivera.ragdemo.mapper;

import cc.ivera.ragdemo.domain.dto.TicketDetailItem;
import cc.ivera.ragdemo.domain.dto.TicketStatusItem;
import cc.ivera.ragdemo.domain.pojo.WorkOrder;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;

import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface WorkOrderMapper extends BaseMapper<WorkOrder> {

    Long countTickets(@Param("assigneeId") String assigneeId,
                      @Param("status") String status,
                      @Param("priority") String priority,
                      @Param("startTime") LocalDateTime startTime,
                      @Param("endTime") LocalDateTime endTime);

    List<TicketStatusItem> statusDistribution(@Param("assigneeId") String assigneeId,
                                              @Param("status") String status,
                                              @Param("priority") String priority,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime);

    List<TicketDetailItem> queryTicketDetails(@Param("assigneeId") String assigneeId,
                                              @Param("status") String status,
                                              @Param("priority") String priority,
                                              @Param("startTime") LocalDateTime startTime,
                                              @Param("endTime") LocalDateTime endTime,
                                              @Param("limit") Integer limit);
}