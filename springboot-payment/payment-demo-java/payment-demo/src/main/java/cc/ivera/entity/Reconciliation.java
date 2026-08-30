package cc.ivera.entity;

import com.baomidou.mybatisplus.annotation.TableName;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Date;

@Data
@TableName("t_reconciliation")
public class Reconciliation extends BaseEntity {

    private LocalDate billDate;

    private String channelCode;

    private Long paymentAppId;

    private String billType;

    private String status;

    private Integer totalCount;

    private Integer matchCount;

    private Integer diffCount;

    private Integer diffAmount;

    private Long channelTotalAmount;

    private Long localTotalAmount;

    private String billHash;

    /** 本次对账消费的渠道账单ID（t_channel_bill.id） */
    private Long billId;

    private String errorMessage;

    private Date startTime;

    private Date endTime;
}
