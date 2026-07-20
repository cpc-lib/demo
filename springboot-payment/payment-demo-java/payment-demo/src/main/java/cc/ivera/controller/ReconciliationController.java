package cc.ivera.controller;

import cc.ivera.dto.ReconciliationExecuteRequest;
import cc.ivera.entity.Reconciliation;
import cc.ivera.entity.ReconciliationDetail;
import cc.ivera.exception.BizException;
import cc.ivera.service.reconciliation.ReconciliationService;
import cc.ivera.vo.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.servlet.http.HttpServletResponse;
import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.io.OutputStream;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;

@CrossOrigin
@RestController
@RequestMapping("/api/reconciliation")
@Api(tags = "支付对账管理")
@Slf4j
@Validated
public class ReconciliationController {

    private final ReconciliationService reconciliationService;

    public ReconciliationController(ReconciliationService reconciliationService) {
        this.reconciliationService = reconciliationService;
    }

    @ApiOperation("手动触发对账")
    @PostMapping("/execute")
    public R<Reconciliation> executeReconciliation(@RequestBody @Valid ReconciliationExecuteRequest request) {
        log.info("触发对账，billDate={}, channelCode={}, paymentAppId={}",
                request.getBillDate(), request.getChannelCode(), request.getPaymentAppId());
        Reconciliation reconciliation = reconciliationService.executeReconciliation(request);
        R<Reconciliation> r = new R<>();
        r.setCode(0);
        r.setMessage("对账任务已提交");
        r.setData(reconciliation);
        return r;
    }

    @ApiOperation("对账记录列表")
    @GetMapping("/list")
    public R<IPage<Reconciliation>> listReconciliation(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") int pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate billDateStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate billDateEnd,
            @RequestParam(required = false) @Pattern(regexp = "WXPAY|ALIPAY", message = "渠道编码只支持WXPAY或ALIPAY") String channelCode,
            @RequestParam(required = false) String status) {
        IPage<Reconciliation> page = reconciliationService.listReconciliation(
                pageNum, pageSize, billDateStart, billDateEnd, channelCode, status);
        return R.ok(page);
    }

    @ApiOperation("对账记录详情")
    @GetMapping("/{id}")
    public R<Reconciliation> getReconciliation(@PathVariable Long id) {
        Reconciliation reconciliation = reconciliationService.getReconciliationById(id);
        if (reconciliation == null) {
            throw new BizException("对账记录不存在");
        }
        return R.ok(reconciliation);
    }

    @ApiOperation("对账明细列表")
    @GetMapping("/{id}/details")
    public R<IPage<ReconciliationDetail>> listDetails(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") int pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") int pageSize,
            @RequestParam(required = false) String diffType) {
        IPage<ReconciliationDetail> page = reconciliationService.listDetails(id, pageNum, pageSize, diffType);
        return R.ok(page);
    }

    @ApiOperation("差异明细列表")
    @GetMapping("/{id}/diff")
    public R<IPage<ReconciliationDetail>> listDiffDetails(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") int pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") int pageSize) {
        IPage<ReconciliationDetail> page = reconciliationService.listDiffDetails(id, pageNum, pageSize);
        return R.ok(page);
    }

    @ApiOperation("导出对账报告")
    @GetMapping("/{id}/export")
    public void exportReconciliation(@PathVariable Long id, HttpServletResponse response) throws Exception {
        String csvContent = reconciliationService.exportReconciliation(id);
        String fileName = URLEncoder.encode("对账报告_" + id + ".csv", StandardCharsets.UTF_8.name()).replaceAll("\\+", "%20");

        response.setContentType("text/csv;charset=UTF-8");
        response.setHeader("Content-Disposition", "attachment; filename*=UTF-8''" + fileName);

        try (OutputStream out = response.getOutputStream()) {
            out.write(csvContent.getBytes(StandardCharsets.UTF_8));
            out.flush();
        }
    }
}
