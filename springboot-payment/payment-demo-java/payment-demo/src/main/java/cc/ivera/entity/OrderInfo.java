package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.baomidou.mybatisplus.annotation.Version;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_order_info")
public class OrderInfo extends BaseEntity {

    private String title;//订单标题

    private String orderNo;//商户订单编号

    private Long userId;//用户id

    private Long productId;//支付产品id

    private Integer totalFee;//订单金额(分)

    private Long goodsAmount;//商品原始总额(分)

    private Long discountAmount;//优惠总额(分)

    private Long freightAmount;//原运费(分)

    private Long payableAmount;//应付总额(分)

    private Long paidAmount;//唯一有效成交实付金额(分)

    private Long refundFrozenAmount;//正常退款冻结金额(分)

    private Long refundedAmount;//正常退款成功金额(分)

    private Long freightRefundFrozenAmount;//运费退款冻结金额(分)

    private Long freightRefundedAmount;//运费退款成功金额(分)

    private String effectivePaymentNo;//唯一有效支付单号

    private String codeUrl;//订单二维码连接

    private String orderStatus;//订单状态

    private String payStatus;//支付状态

    private String fulfillmentStatus;//履约状态

    private String refundStatus;//正常售后汇总状态

    private String closeReason;//关单原因

    private Date expiresAt;//支付截止时间

    private Date closeRequestedAt;//关单门闩时间

    private Date paidAt;//有效成交时间

    private String paymentType;//支付方式

    private Long paymentAppId;//支付应用ID

    private String paymentChannelCode;//支付渠道编码

    private String checkoutRequestId;//购物车结算幂等请求号

    @Version
    private Integer version;//乐观锁版本号
}
