package cc.ivera.mapper;

import cc.ivera.entity.PaymentOrder;
import cc.ivera.enums.PaymentOrderStatus;
import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import org.apache.ibatis.annotations.Param;

import java.util.List;

public interface PaymentOrderMapper extends BaseMapper<PaymentOrder> {

    PaymentOrder selectByPaymentNo(@Param("paymentNo") String paymentNo);

    PaymentOrder selectByPaymentNoForUpdate(@Param("paymentNo") String paymentNo);

    PaymentOrder selectByOrderNoAndRequestIdForUpdate(
            @Param("orderNo") String orderNo,
            @Param("requestId") String requestId
    );

    PaymentOrder selectUncertainByOrderNoForUpdate(@Param("orderNo") String orderNo);

    PaymentOrder selectReusablePendingForUpdate(
            @Param("orderNo") String orderNo,
            @Param("paymentAppId") Long paymentAppId,
            @Param("protocol") String protocol
    );

    PaymentOrder selectByPaymentAppIdAndMerchantOrderNo(
            @Param("paymentAppId") Long paymentAppId,
            @Param("channelMerchantOrderNo") String channelMerchantOrderNo
    );

    PaymentOrder selectByPaymentAppIdAndChannelTransactionId(
            @Param("paymentAppId") Long paymentAppId,
            @Param("channelTransactionId") String channelTransactionId
    );

    List<PaymentOrder> selectByOrderNo(@Param("orderNo") String orderNo);

    int updateStatus(
            @Param("paymentNo") String paymentNo,
            @Param("currentStatus") PaymentOrderStatus currentStatus,
            @Param("targetStatus") PaymentOrderStatus targetStatus
    );

    int updateCreationResult(
            @Param("paymentNo") String paymentNo,
            @Param("currentStatus") PaymentOrderStatus currentStatus,
            @Param("targetStatus") PaymentOrderStatus targetStatus,
            @Param("paymentParams") String paymentParams
    );
}
