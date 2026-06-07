package cc.ivera.controller;

import cc.ivera.dto.RefundApproveRequest;
import cc.ivera.entity.RefundInfo;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@CrossOrigin
@Api(tags = "退款申请单管理")
@RestController
@RequestMapping("/api/refund-info")
public class RefundInfoController {

    @Resource
    private RefundApplicationService refundApplicationService;

    @ApiOperation("退款申请单列表")
    @GetMapping("/list")
    public R list() {
        List<RefundInfo> list = refundApplicationService.listAll();
        return R.ok().data("list", list);
    }

    @ApiOperation("按订单号查询退款申请单")
    @GetMapping("/list/{orderNo}")
    public R listByOrderNo(@PathVariable String orderNo) {
        List<RefundInfo> list = refundApplicationService.listByOrderNo(orderNo);
        return R.ok().data("list", list);
    }

    @ApiOperation("审核通过并执行退款")
    @PostMapping("/approve/{refundNo}")
    public R approve(@PathVariable String refundNo, @RequestBody(required = false) RefundApproveRequest request) throws Exception {
        refundApplicationService.approve(refundNo, request == null ? null : request.getApproveRemark());
        return R.ok().setMessage("审核通过，退款已提交处理");
    }

    @ApiOperation("审核拒绝退款申请")
    @PostMapping("/reject/{refundNo}")
    public R reject(@PathVariable String refundNo, @RequestBody(required = false) RefundApproveRequest request) {
        refundApplicationService.reject(refundNo, request == null ? null : request.getApproveRemark());
        return R.ok().setMessage("退款申请已拒绝");
    }
}
