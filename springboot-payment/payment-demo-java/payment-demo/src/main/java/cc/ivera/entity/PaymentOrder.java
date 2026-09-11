package cc.ivera.entity;

import cc.ivera.enums.PaymentDisposition;
import cc.ivera.enums.PaymentOrderStatus;
import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Date;

@Data
@EqualsAndHashCode(callSuper = true)
@TableName("t_payment_order")
public class PaymentOrder extends BaseEntity {

    private String paymentNo;

    private String orderNo;

    private Long paymentAppId;

    private String channel;

    private String protocol;

    private String channelMerchantOrderNo;

    private String channelTransactionId;

    private Long requestAmount;

    private Long paidAmount;

    private Long payerCashAmount;

    private Long receiptAmount;

    private String currency;

    private PaymentOrderStatus status;

    private PaymentDisposition disposition;

    private String requestId;

    private String requestFingerprint;

    private String paymentParams;

    private Long refundFrozenAmount;

    private Long refundedAmount;

    private Date expiresAt;

    private Date paidAt;
}

