package cc.ivera.dto;

import lombok.Data;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
public class PaymentAttemptRequest {

    @NotNull(message = "支付应用不能为空")
    private Long paymentAppId;

    @NotBlank(message = "支付协议不能为空")
    @Size(max = 32, message = "支付协议不能超过32个字符")
    private String protocol;

    @NotBlank(message = "支付请求号不能为空")
    @Size(max = 64, message = "支付请求号不能超过64个字符")
    private String requestId;
}
