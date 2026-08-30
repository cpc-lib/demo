package cc.ivera.controller;

import cc.ivera.dto.RefundRequest;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.security.AuthContext;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.Map;

@CrossOrigin
@Api(tags = "退款申请")
@RestController
@Validated
public class RefundApplicationController {

    private final RefundApplicationService refundApplicationService;

    private final OrderInfoService orderInfoService;

    @Autowired
    public RefundApplicationController(
            RefundApplicationService refundApplicationService,
            OrderInfoService orderInfoService
    ) {
        this.refundApplicationService = refundApplicationService;
        this.orderInfoService = orderInfoService;
    }

    public RefundApplicationController(RefundApplicationService refundApplicationService) {
        this(refundApplicationService, null);
    }

    @ApiOperation("申请退款")
    @PostMapping({
            "/api/refund-info/apply",
            "/api/wx-pay/refunds",
            "/api/ali-pay/trade/refund"
    })
    public R<Map<String, Object>> apply(@Valid @RequestBody RefundRequest request) {
        checkOwnership(request.getOrderNo());
        refundApplicationService.createApplication(request.getOrderNo(), request.getRefundAmount(), request.getReason());
        return R.ok().setMessage("退款申请单创建成功，待审核");
    }

    @ApiOperation("申请退款，兼容旧接口")
    @PostMapping({
            "/api/refund-info/apply/{orderNo}/{reason}",
            "/api/wx-pay/refunds/{orderNo}/{reason}",
            "/api/ali-pay/trade/refund/{orderNo}/{reason}"
    })
    public R<Map<String, Object>> applyLegacy(
            @PathVariable @NotBlank(message = "订单号不能为空") @Size(max = 50, message = "订单号长度不能超过50个字符") String orderNo,
            @PathVariable @NotBlank(message = "退款原因不能为空") @Size(max = 50, message = "退款原因长度不能超过50个字符") String reason) {
        checkOwnership(orderNo);
        refundApplicationService.createApplication(orderNo, null, reason);
        return R.ok().setMessage("退款申请单创建成功，待审核");
    }

    private void checkOwnership(String orderNo) {
        if (orderInfoService != null) {
            orderInfoService.getOrderForUser(orderNo, AuthContext.requireUser());
        }
    }
}
