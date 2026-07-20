package cc.ivera.controller;

import cc.ivera.dto.reconciliation.*;
import cc.ivera.entity.reconciliation.ReconciliationDetail;
import cc.ivera.entity.reconciliation.ReconciliationDiscrepancy;
import cc.ivera.service.reconciliation.ReconciliationBatchService;
import cc.ivera.vo.R;
import cc.ivera.vo.reconciliation.ReconciliationBatchVO;
import cc.ivera.vo.reconciliation.ReconciliationProgressVO;
import cc.ivera.vo.reconciliation.ReconciliationSummaryVO;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import javax.validation.Valid;

@CrossOrigin
@RestController
@RequestMapping("/api/reconciliation")
@Api(tags = "对账管理")
@Slf4j
@Validated
public class ReconciliationController {

    private final ReconciliationBatchService reconciliationBatchService;

    public ReconciliationController(ReconciliationBatchService reconciliationBatchService) {
        this.reconciliationBatchService = reconciliationBatchService;
    }

    @ApiOperation("创建对账批次")
    @PostMapping("/batch/create")
    public R<ReconciliationBatchVO> createBatch(@RequestBody @Valid ReconciliationBatchCreateDTO dto) {
        log.info("创建对账批次，channelCode={}, billDate={}", dto.getChannelCode(), dto.getBillDate());
        return R.ok(reconciliationBatchService.createBatch(dto));
    }

    @ApiOperation("对账批次分页列表")
    @GetMapping("/batch/list")
    public R<IPage<ReconciliationBatchVO>> pageBatches(@Valid ReconciliationBatchQueryDTO dto) {
        return R.ok(reconciliationBatchService.pageBatches(dto));
    }

    @ApiOperation("对账批次详情")
    @GetMapping("/batch/{batchNo}")
    public R<ReconciliationBatchVO> getBatchDetail(@PathVariable String batchNo) {
        return R.ok(reconciliationBatchService.getBatchByNo(batchNo));
    }

    @ApiOperation("手动执行对账（异步）")
    @PostMapping("/batch/{batchNo}/execute")
    public R<String> executeBatch(@PathVariable String batchNo) {
        log.info("手动触发对账执行，batchNo={}", batchNo);
        reconciliationBatchService.asyncExecuteBatch(batchNo);
        return R.ok("对账任务已提交，后台执行中");
    }

    @ApiOperation("对账执行进度查询")
    @GetMapping("/batch/{batchNo}/progress")
    public R<ReconciliationProgressVO> getProgress(@PathVariable String batchNo) {
        return R.ok(reconciliationBatchService.getProgress(batchNo));
    }

    @ApiOperation("对账汇总统计")
    @GetMapping("/summary")
    public R<ReconciliationSummaryVO> getSummary() {
        return R.ok(reconciliationBatchService.getSummary());
    }

    @ApiOperation("对账明细分页列表")
    @GetMapping("/detail/list/{batchNo}")
    public R<IPage<ReconciliationDetail>> pageDetails(@PathVariable String batchNo,
            @RequestParam(required = false) String matchStatus,
            @RequestParam(required = false) String discrepancyType,
            @RequestParam(required = false) String tradeType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        ReconciliationDetailQueryDTO dto = new ReconciliationDetailQueryDTO();
        dto.setBatchNo(batchNo);
        dto.setMatchStatus(matchStatus);
        dto.setDiscrepancyType(discrepancyType);
        dto.setTradeType(tradeType);
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        return R.ok(reconciliationBatchService.pageDetails(dto));
    }

    @ApiOperation("差异单分页列表")
    @GetMapping("/discrepancy/list/{batchNo}")
    public R<IPage<ReconciliationDiscrepancy>> pageDiscrepancies(@PathVariable String batchNo,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String discrepancyType,
            @RequestParam(defaultValue = "1") Integer pageNum,
            @RequestParam(defaultValue = "50") Integer pageSize) {
        ReconciliationDiscrepancyQueryDTO dto = new ReconciliationDiscrepancyQueryDTO();
        dto.setBatchNo(batchNo);
        dto.setStatus(status);
        dto.setDiscrepancyType(discrepancyType);
        dto.setPageNum(pageNum);
        dto.setPageSize(pageSize);
        return R.ok(reconciliationBatchService.pageDiscrepancies(dto));
    }

    @ApiOperation("处理差异单")
    @PostMapping("/discrepancy/{discrepancyId}/resolve")
    public R<String> resolveDiscrepancy(@PathVariable Long discrepancyId,
            @RequestBody @Valid ReconciliationDiscrepancyResolveDTO dto) {
        log.info("处理差异单，discrepancyId={}", discrepancyId);
        reconciliationBatchService.resolveDiscrepancy(discrepancyId, dto);
        return R.ok("差异已处理");
    }
}
