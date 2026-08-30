package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;

import java.time.LocalDate;
import java.util.Date;

/**
 * 渠道账单（对账依据）
 */
@Data
@TableName("t_channel_bill")
public class ChannelBill extends BaseEntity {

    private LocalDate billDate;

    private String channelCode;

    private Long paymentAppId;

    private String billType;

    /** 账单来源：AUTO_DOWNLOAD（API拉取）/ MANUAL_UPLOAD（手动上传） */
    private String billSource;

    /** 账单状态：IMPORTED（已导入） */
    private String status;

    private Integer recordCount;

    private Long totalAmount;

    private String billHash;

    private String fileName;

    /** 账单原文（CSV），不对外返回 */
    @JsonIgnore
    private String billContent;

    private Date importTime;
}
