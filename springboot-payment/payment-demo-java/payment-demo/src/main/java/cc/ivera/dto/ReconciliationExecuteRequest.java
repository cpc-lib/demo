package cc.ivera.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Pattern;
import javax.validation.constraints.Size;

@Data
public class ReconciliationExecuteRequest {

    @NotBlank(message = "对账日期不能为空")
    @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "对账日期格式必须为yyyy-MM-dd")
    private String billDate;

    @NotBlank(message = "渠道编码不能为空")
    @Pattern(regexp = "WXPAY|ALIPAY", message = "渠道编码只支持WXPAY或ALIPAY")
    private String channelCode;

    private Long paymentAppId;

    @Size(max = 16, message = "账单子类型长度不能超过16个字符")
    private String billType;

    private Boolean force;
}
