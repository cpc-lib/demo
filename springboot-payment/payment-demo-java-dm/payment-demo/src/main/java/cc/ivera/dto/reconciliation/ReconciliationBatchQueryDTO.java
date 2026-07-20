package cc.ivera.dto.reconciliation;

import lombok.Data;

import javax.validation.constraints.Pattern;

@Data
public class ReconciliationBatchQueryDTO {

    @Pattern(regexp = "WXPAY|ALIPAY", message = "渠道类型只支持WXPAY或ALIPAY")
    private String channelCode;

    private String status;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "账单日期格式必须为yyyy-MM-dd")
    private String billDate;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "账单开始日期格式必须为yyyy-MM-dd")
    private String billDateStart;

    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "账单结束日期格式必须为yyyy-MM-dd")
    private String billDateEnd;

    private Integer pageNum = 1;

    private Integer pageSize = 20;
}
