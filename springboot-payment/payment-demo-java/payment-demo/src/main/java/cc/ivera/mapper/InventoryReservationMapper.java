package cc.ivera.mapper;

import cc.ivera.entity.InventoryReservation;
import cc.ivera.enums.ReservationStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface InventoryReservationMapper extends BaseMapper<InventoryReservation> {

    InventoryReservation selectByOrderItemIdForUpdate(@Param("orderItemId") Long orderItemId);

    List<InventoryReservation> selectByOrderNoForUpdate(@Param("orderNo") String orderNo);

    int updateStatus(
            @Param("id") Long id,
            @Param("currentStatus") ReservationStatus currentStatus,
            @Param("targetStatus") ReservationStatus targetStatus
    );
}

