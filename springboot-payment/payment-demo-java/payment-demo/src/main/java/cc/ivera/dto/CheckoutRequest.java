package cc.ivera.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import javax.validation.constraints.Size;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class CheckoutRequest {

    @NotNull(message = "支付应用不能为空")
    private Long paymentAppId;

    @NotBlank(message = "结算请求号不能为空")
    @Size(max = 64, message = "结算请求号不能超过64个字符")
    private String checkoutRequestId;
}
