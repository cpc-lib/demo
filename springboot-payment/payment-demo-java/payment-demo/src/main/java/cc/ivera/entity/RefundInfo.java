package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.util.Date;

@Data
@TableName("t_refund_info")
public class RefundInfo extends BaseEntity {

    private String orderNo;//商品订单编号

    private String refundNo;//退款单编号

    private String refundId;//支付系统退款单号

    private String paymentNo;//原支付单号

    private String refundSource;//USER、SYSTEM

    private String refundType;//类型化退款场景

    private String applicationRequestId;//退款申请幂等请求号

    private String requestFingerprint;//规范化请求指纹

    private String systemRefundKey;//仅系统退款填充的唯一业务键

    private Integer totalFee;//原订单金额(分)

    private Integer refund;//退款金额(分)

    private Long goodsRefundAmount;//商品退款金额(分)

    private Long freightRefundAmount;//原运费退款金额(分)

    private Long refundAmount;//新模型退款总额(分)

    private String reason;//退款原因

    private String approvalStatus;//审核状态

    private String approveRemark;//审核备注

    private Date approvedTime;//审核时间

    private String refundStatus;//退款单状态

    private String channelRefundId;//渠道退款单号

    private String channelStatus;//渠道退款状态

    private Date requestedAt;//申请时间

    private Date successTime;//成功时间

    private String contentReturn;//申请退款返回参数

    private String contentNotify;//退款结果通知参数
}
