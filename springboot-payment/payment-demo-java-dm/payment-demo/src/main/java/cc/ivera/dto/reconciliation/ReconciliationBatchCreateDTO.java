package cc.ivera.dto.reconciliation;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;

@Data
public class ReconciliationBatchCreateDTO {

    @NotBlank(message = "渠道编码不能为空")
    @Pattern(regexp = "WXPAY|ALIPAY", message = "渠道类型只支持WXPAY或ALIPAY")
    private String channelCode;

    private Long paymentAppId;

    @NotBlank(message = "账单日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "账单日期格式必须为yyyy-MM-dd")
    private String billDate;
}
