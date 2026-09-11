package cc.ivera.controller;

import cc.ivera.dto.PaymentAttemptRequest;
import cc.ivera.entity.PaymentOrder;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.payment.PaymentAttemptService;
import cc.ivera.service.payment.PaymentInitiationService;
import cc.ivera.vo.PaymentOrderView;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.stream.Collectors;

@Api(tags = "订单支付尝试")
@RestController
@RequestMapping("/api/order-info")
@Validated
public class PaymentOrderController {

    private final PaymentInitiationService paymentInitiationService;

    private final PaymentAttemptService paymentAttemptService;

    public PaymentOrderController(
            PaymentInitiationService paymentInitiationService,
            PaymentAttemptService paymentAttemptService
    ) {
        this.paymentInitiationService = paymentInitiationService;
        this.paymentAttemptService = paymentAttemptService;
    }

    @ApiOperation("创建或重放支付尝试")
    @PostMapping("/{orderNo}/payments")
    public R<PaymentOrderView> create(
            @PathVariable
            @NotBlank(message = "订单号不能为空")
            @Size(max = 64, message = "订单号不能超过64个字符") String orderNo,
            @Valid @RequestBody PaymentAttemptRequest request,
            HttpServletRequest servletRequest
    ) {
        AuthUser user = AuthContext.requireShoppingUser();
        PaymentOrder paymentOrder = paymentInitiationService.initiate(
                orderNo,
                request.getPaymentAppId(),
                request.getProtocol(),
                request.getRequestId(),
                servletRequest.getRemoteAddr(),
                user
        );
        return R.ok(PaymentOrderView.from(paymentOrder));
    }

    @ApiOperation("查询订单的全部支付尝试")
    @GetMapping("/{orderNo}/payments")
    public R<List<PaymentOrderView>> list(
            @PathVariable
            @NotBlank(message = "订单号不能为空")
            @Size(max = 64, message = "订单号不能超过64个字符") String orderNo
    ) {
        List<PaymentOrderView> views = paymentAttemptService
                .listForOrder(orderNo, AuthContext.requireUser())
                .stream()
                .map(PaymentOrderView::from)
                .collect(Collectors.toList());
        return R.ok(views);
    }
}
