package cc.ivera.model.vo;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class DocumentManageVo implements Serializable {
    /**
     * 原账单金额
     */
    private BigDecimal bill_amount;
    /**
     * 申请修改金额
     */
    private BigDecimal adjust_bill_amount;
    /**
     * 原账单开始时间
     */
    private Date bill_start_date;
    /**
     * 原账单结束时间
     */
    private Date bill_end_date;
    /**
     * 申请修改开始时间
     */
    private Date adjust_bill_start_date;
    /**
     * 申请修改结束时间
     */
    private Date adjust_bill_end_date;
    /**
     * 合同管理ID
     */
    private String contract_managet_id;
    /**
     * 账单id
     */
    private String document_manage_id;
    /**
     * 操作
     */
    private String operation;
}
