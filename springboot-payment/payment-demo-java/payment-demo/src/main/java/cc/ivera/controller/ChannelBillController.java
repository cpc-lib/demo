package cc.ivera.controller;

import cc.ivera.dto.ChannelBillImportRequest;
import cc.ivera.entity.ChannelBill;
import cc.ivera.service.impl.reconciliation.ChannelBillRecord;
import cc.ivera.service.reconciliation.ChannelBillService;
import cc.ivera.vo.R;
import com.baomidou.mybatisplus.core.metadata.IPage;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import javax.validation.Valid;
import javax.validation.constraints.Max;
import javax.validation.constraints.Min;
import javax.validation.constraints.Pattern;
import java.time.LocalDate;

@CrossOrigin
@RestController
@RequestMapping("/api/bill")
@Api(tags = "渠道账单管理")
@Slf4j
@Validated
public class ChannelBillController {

    private final ChannelBillService channelBillService;

    public ChannelBillController(ChannelBillService channelBillService) {
        this.channelBillService = channelBillService;
    }

    @ApiOperation("自动拉取渠道账单并导入")
    @PostMapping("/auto-fetch")
    public R<ChannelBill> importFromChannel(@RequestBody @Valid ChannelBillImportRequest request) {
        log.info("自动拉取渠道账单，billDate={}, channelCode={}, billType={}, force={}",
                request.getBillDate(), request.getChannelCode(), request.getBillType(), request.getForce());
        ChannelBill bill = channelBillService.importFromChannel(request);
        return R.ok(bill);
    }

    @ApiOperation("手动上传账单文件导入")
    @PostMapping("/upload")
    public R<ChannelBill> uploadBill(
            @RequestParam("file") MultipartFile file,
            @RequestParam @Pattern(regexp = "\\d{4}-\\d{2}-\\d{2}", message = "账单日期格式必须为yyyy-MM-dd") String billDate,
            @RequestParam @Pattern(regexp = "WXPAY|ALIPAY", message = "渠道编码只支持WXPAY或ALIPAY") String channelCode,
            @RequestParam(required = false) String billType,
            @RequestParam(required = false) Boolean force) {
        log.info("手动上传渠道账单，billDate={}, channelCode={}, fileName={}", billDate, channelCode, file.getOriginalFilename());
        ChannelBill bill = channelBillService.uploadBill(file, billDate, channelCode, billType, force);
        return R.ok(bill);
    }

    @ApiOperation("已导入账单列表")
    @GetMapping("/list")
    public R<IPage<ChannelBill>> listBills(
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") int pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") int pageSize,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate billDateStart,
            @RequestParam(required = false) @DateTimeFormat(pattern = "yyyy-MM-dd") LocalDate billDateEnd,
            @RequestParam(required = false) @Pattern(regexp = "WXPAY|ALIPAY", message = "渠道编码只支持WXPAY或ALIPAY") String channelCode,
            @RequestParam(required = false) String billSource) {
        IPage<ChannelBill> page = channelBillService.listBills(pageNum, pageSize,
                billDateStart, billDateEnd, channelCode, billSource);
        return R.ok(page);
    }

    @ApiOperation("账单详情")
    @GetMapping("/{id}")
    public R<ChannelBill> getBill(@PathVariable Long id) {
        return R.ok(channelBillService.getBillById(id));
    }

    @ApiOperation("账单解析记录列表")
    @GetMapping("/{id}/records")
    public R<IPage<ChannelBillRecord>> listRecords(
            @PathVariable Long id,
            @RequestParam(defaultValue = "1") @Min(value = 1, message = "页码最小为1") int pageNum,
            @RequestParam(defaultValue = "20") @Min(value = 1, message = "每页条数最小为1") @Max(value = 100, message = "每页条数最大为100") int pageSize) {
        return R.ok(channelBillService.listRecords(id, pageNum, pageSize));
    }

    @ApiOperation("删除账单")
    @DeleteMapping("/{id}")
    public R<Void> deleteBill(@PathVariable Long id) {
        channelBillService.deleteBill(id);
        R<Void> r = new R<>();
        r.setCode(0);
        r.setMessage("删除成功");
        return r;
    }
}
