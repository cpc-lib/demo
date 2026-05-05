package cc.ivera.controller;

import cc.ivera.dto.RefundApproveRequest;
import cc.ivera.entity.RefundInfo;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;
import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@CrossOrigin
@Api(tags = "退款申请单管理")
@RestController
@RequestMapping("/api/refund-info")
@Validated
public class RefundInfoController {

    private final RefundApplicationService refundApplicationService;

    public RefundInfoController(
        RefundApplicationService refundApplicationService
    ) {
        this.refundApplicationService = refundApplicationService;
    }

    @ApiOperation("退款申请单列表")
    @GetMapping("/list")
    public R<Map<String, Object>> list() {
        List<RefundInfo> list = refundApplicationService.listAll();
        return R.ok().data("list", list);
    }

    @ApiOperation("按订单号查询退款申请单")
    @GetMapping("/list/{orderNo}")
    public R<Map<String, Object>> listByOrderNo(@PathVariable @NotBlank(message = "订单号不能为空") @Size(max = 50, message = "订单号长度不能超过50个字符") String orderNo) {
        List<RefundInfo> list = refundApplicationService.listByOrderNo(orderNo);
        return R.ok().data("list", list);
    }

    @ApiOperation("审核通过并执行退款")
    @PostMapping("/approve/{refundNo}")
    public R<Map<String, Object>> approve(@PathVariable @NotBlank(message = "退款单号不能为空") @Size(max = 50, message = "退款单号长度不能超过50个字符") String refundNo,
                                          @Valid @RequestBody(required = false) RefundApproveRequest request) {
        refundApplicationService.approve(refundNo, request == null ? null : request.getApproveRemark());
        return R.ok().setMessage("审核通过，退款已提交处理");
    }

    @ApiOperation("审核拒绝退款申请")
    @PostMapping("/reject/{refundNo}")
    public R<Map<String, Object>> reject(@PathVariable @NotBlank(message = "退款单号不能为空") @Size(max = 50, message = "退款单号长度不能超过50个字符") String refundNo,
                                         @Valid @RequestBody(required = false) RefundApproveRequest request) {
        refundApplicationService.reject(refundNo, request == null ? null : request.getApproveRemark());
        return R.ok().setMessage("退款申请已拒绝");
    }
}
