package cc.ivera.service.reconciliation;

import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.dto.reconciliation.ReconciliationBatchCreateDTO;
import cc.ivera.dto.reconciliation.ReconciliationBatchQueryDTO;
import cc.ivera.dto.reconciliation.ReconciliationDetailQueryDTO;
import cc.ivera.dto.reconciliation.ReconciliationDiscrepancyQueryDTO;
import cc.ivera.dto.reconciliation.ReconciliationDiscrepancyResolveDTO;
import cc.ivera.entity.reconciliation.ReconciliationBatch;
import cc.ivera.entity.reconciliation.ReconciliationDetail;
import cc.ivera.entity.reconciliation.ReconciliationDiscrepancy;
import cc.ivera.enums.reconciliation.BatchStatus;
import cc.ivera.enums.reconciliation.DiscrepancyStatus;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.reconciliation.ReconciliationBatchMapper;
import cc.ivera.mapper.reconciliation.ReconciliationDetailMapper;
import cc.ivera.mapper.reconciliation.ReconciliationDiscrepancyMapper;
import cc.ivera.mq.ReconciliationExecuteProducer;
import cc.ivera.service.reconciliation.channel.ChannelReconciliationStrategy;
import cc.ivera.service.reconciliation.channel.ChannelReconciliationStrategyFactory;
import cc.ivera.util.OrderNoUtils;
import cc.ivera.vo.reconciliation.ReconciliationBatchVO;
import cc.ivera.vo.reconciliation.ReconciliationProgressVO;
import cc.ivera.vo.reconciliation.ReconciliationSummaryVO;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.metadata.IPage;
import com.baomidou.mybatisplus.extension.plugins.pagination.Page;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.Arrays;
import java.util.Date;
import java.util.List;

@Service
@Slf4j
public class ReconciliationBatchServiceImpl implements ReconciliationBatchService {

    private static final String LOCK_KEY_PREFIX = "reconciliation:batch:lock:";

    private final ReconciliationBatchMapper batchMapper;

    private final ReconciliationDetailMapper detailMapper;

    private final ReconciliationDiscrepancyMapper discrepancyMapper;

    private final ReconciliationMatchService reconciliationMatchService;

    private final ChannelReconciliationStrategyFactory strategyFactory;

    private final DistributedLockTemplate lockTemplate;

    private final PaymentConfigLoader paymentConfigLoader;

    private final ReconciliationExecuteProducer executeProducer;

    public ReconciliationBatchServiceImpl(
        ReconciliationBatchMapper batchMapper,
        ReconciliationDetailMapper detailMapper,
        ReconciliationDiscrepancyMapper discrepancyMapper,
        ReconciliationMatchService reconciliationMatchService,
        ChannelReconciliationStrategyFactory strategyFactory,
        DistributedLockTemplate lockTemplate,
        PaymentConfigLoader paymentConfigLoader,
        ReconciliationExecuteProducer executeProducer
    ) {
        this.batchMapper = batchMapper;
        this.detailMapper = detailMapper;
        this.discrepancyMapper = discrepancyMapper;
        this.reconciliationMatchService = reconciliationMatchService;
        this.strategyFactory = strategyFactory;
        this.lockTemplate = lockTemplate;
        this.paymentConfigLoader = paymentConfigLoader;
        this.executeProducer = executeProducer;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public ReconciliationBatchVO createBatch(ReconciliationBatchCreateDTO dto) {
        log.info("创建对账批次，channelCode={}, paymentAppId={}, billDate={}",
                dto.getChannelCode(), dto.getPaymentAppId(), dto.getBillDate());

        validateBillDate(dto.getBillDate());
        validateChannelCode(dto.getChannelCode());

        Long appId = dto.getPaymentAppId();
        if (appId == null) {
            appId = resolveDefaultAppId(dto.getChannelCode());
        }

        ReconciliationBatch batch = createBatchInternal(dto.getChannelCode(), appId, dto.getBillDate());
        return convertToBatchVO(batch);
    }

    @Override
    public ReconciliationBatchVO getBatchByNo(String batchNo) {
        ReconciliationBatch batch = getByBatchNoOrThrow(batchNo);
        return convertToBatchVO(batch);
    }

    @Override
    public IPage<ReconciliationBatchVO> pageBatches(ReconciliationBatchQueryDTO dto) {
        QueryWrapper<ReconciliationBatch> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(dto.getChannelCode())) {
            queryWrapper.eq("channel_code", dto.getChannelCode());
        }
        if (StringUtils.hasText(dto.getStatus())) {
            queryWrapper.eq("status", dto.getStatus());
        }
        if (StringUtils.hasText(dto.getBillDate())) {
            queryWrapper.eq("bill_date", dto.getBillDate());
        }
        if (StringUtils.hasText(dto.getBillDateStart())) {
            queryWrapper.ge("bill_date", dto.getBillDateStart());
        }
        if (StringUtils.hasText(dto.getBillDateEnd())) {
            queryWrapper.le("bill_date", dto.getBillDateEnd());
        }
        queryWrapper.orderByDesc("create_time");

        Page<ReconciliationBatch> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        IPage<ReconciliationBatch> batchPage = batchMapper.selectPage(page, queryWrapper);

        return batchPage.convert(this::convertToBatchVO);
    }

    @Override
    public void asyncExecuteBatch(String batchNo) {
        log.info("异步执行对账批次，batchNo={}", batchNo);

        ReconciliationBatch batch = getByBatchNoOrThrow(batchNo);
        if (!BatchStatus.CREATED.name().equals(batch.getStatus())) {
            throw new BizException("对账批次状态不正确，当前状态=" + batch.getStatus());
        }

        executeProducer.sendExecute(batchNo);
    }

    @Override
    public void executeBatch(String batchNo) {
        log.info("执行对账批次，batchNo={}", batchNo);

        String lockKey = LOCK_KEY_PREFIX + batchNo;
        lockTemplate.execute(lockKey, 3000L, 300000L, () -> {
            try {
                doExecuteBatch(batchNo);
            } catch (Exception e) {
                log.error("执行对账批次失败，batchNo={}", batchNo, e);
                updateBatchStatus(batchNo, BatchStatus.FAILED, e.getMessage());
                throw new BizException("执行对账批次失败：" + e.getMessage(), e);
            }
        });
    }

    @Override
    public ReconciliationProgressVO getProgress(String batchNo) {
        ReconciliationBatch batch = getByBatchNoOrThrow(batchNo);

        ReconciliationProgressVO vo = new ReconciliationProgressVO();
        vo.setBatchNo(batchNo);
        vo.setStatus(batch.getStatus());

        String status = batch.getStatus();
        if (BatchStatus.CREATED.name().equals(status)) {
            vo.setCurrentStep("等待执行");
            vo.setProgressPercent(0);
            vo.setMessage("对账批次已创建，等待执行");
        } else if (BatchStatus.BILL_DOWNLOADED.name().equals(status)) {
            vo.setCurrentStep("账单下载完成");
            vo.setProgressPercent(40);
            vo.setMessage("渠道账单已下载并解析完成");
        } else if (BatchStatus.LOCAL_COLLECTED.name().equals(status)) {
            vo.setCurrentStep("本地数据采集完成");
            vo.setProgressPercent(60);
            vo.setMessage("本地交易数据已采集完成");
        } else if (BatchStatus.MATCHED.name().equals(status)) {
            vo.setCurrentStep("对账匹配完成");
            vo.setProgressPercent(80);
            vo.setMessage("对账匹配已完成，正在检查差异");
        } else if (BatchStatus.DISCREPANCY_PENDING.name().equals(status)) {
            vo.setCurrentStep("差异待处理");
            vo.setProgressPercent(90);
            vo.setMessage("存在差异待处理，差异数=" + batch.getDiscrepancyCount());
        } else if (BatchStatus.RESOLVED.name().equals(status)) {
            vo.setCurrentStep("差异已处理");
            vo.setProgressPercent(95);
            vo.setMessage("所有差异已处理完成");
        } else if (BatchStatus.COMPLETED.name().equals(status)) {
            vo.setCurrentStep("已完成");
            vo.setProgressPercent(100);
            vo.setMessage("对账批次已全部完成");
        } else if (BatchStatus.FAILED.name().equals(status)) {
            vo.setCurrentStep("执行失败");
            vo.setProgressPercent(0);
            vo.setMessage("执行失败：" + batch.getFailureReason());
        } else {
            vo.setCurrentStep("未知状态");
            vo.setProgressPercent(0);
            vo.setMessage("当前状态：" + status);
        }

        return vo;
    }

    @Override
    public ReconciliationSummaryVO getSummary() {
        ReconciliationSummaryVO vo = new ReconciliationSummaryVO();
        String today = LocalDate.now().toString();

        String tomorrow = LocalDate.now().plusDays(1).toString();
        QueryWrapper<ReconciliationBatch> todayWrapper = new QueryWrapper<>();
        todayWrapper.ge("create_time", today).lt("create_time", tomorrow);
        List<ReconciliationBatch> todayBatches = batchMapper.selectList(todayWrapper);
        vo.setTodayBatchCount(todayBatches.size());

        long completedCount = todayBatches.stream()
                .filter(b -> BatchStatus.COMPLETED.name().equals(b.getStatus()))
                .count();
        vo.setTodayCompletedCount((int) completedCount);

        long failedCount = todayBatches.stream()
                .filter(b -> BatchStatus.FAILED.name().equals(b.getStatus()))
                .count();
        vo.setTodayFailedCount((int) failedCount);

        QueryWrapper<ReconciliationDiscrepancy> discrepancyWrapper = new QueryWrapper<>();
        discrepancyWrapper.eq("status", DiscrepancyStatus.OPEN.name());
        Integer pendingCount = discrepancyMapper.selectCount(discrepancyWrapper);
        vo.setPendingDiscrepancyCount(pendingCount != null ? pendingCount : 0);

        String sevenDaysAgo = LocalDate.now().minusDays(7).toString();
        QueryWrapper<ReconciliationBatch> sevenDaysWrapper = new QueryWrapper<>();
        sevenDaysWrapper.ge("bill_date", sevenDaysAgo);
        List<ReconciliationBatch> sevenDaysBatches = batchMapper.selectList(sevenDaysWrapper);

        long totalAmount = sevenDaysBatches.stream()
                .mapToLong(b -> b.getChannelTotalAmount() != null ? b.getChannelTotalAmount() : 0L)
                .sum();
        vo.setLast7DaysTransactionAmount(totalAmount);

        long matchedAmount = sevenDaysBatches.stream()
                .mapToLong(b -> b.getMatchedAmount() != null ? b.getMatchedAmount() : 0L)
                .sum();
        vo.setLast7DaysMatchedAmount(matchedAmount);

        int discrepancyCount = sevenDaysBatches.stream()
                .mapToInt(b -> b.getDiscrepancyCount() != null ? b.getDiscrepancyCount() : 0)
                .sum();
        vo.setLast7DaysDiscrepancyCount(discrepancyCount);

        return vo;
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void resolveDiscrepancy(Long discrepancyId, ReconciliationDiscrepancyResolveDTO dto) {
        log.info("处理差异单，discrepancyId={}, resolveRemark={}", discrepancyId, dto.getResolveRemark());

        if (discrepancyId == null) {
            throw new BizException("差异单ID不能为空");
        }
        if (dto == null || !StringUtils.hasText(dto.getResolveRemark())) {
            throw new BizException("处理备注不能为空");
        }

        resolveDiscrepancyInternal(discrepancyId, dto.getResolveRemark());
    }

    @Override
    public IPage<ReconciliationDetail> pageDetails(ReconciliationDetailQueryDTO dto) {
        if (dto.getBatchNo() == null || dto.getBatchNo().trim().isEmpty()) {
            throw new BizException("批次号不能为空");
        }

        QueryWrapper<ReconciliationDetail> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("batch_no", dto.getBatchNo());
        if (StringUtils.hasText(dto.getMatchStatus())) {
            queryWrapper.eq("match_status", dto.getMatchStatus());
        }
        if (StringUtils.hasText(dto.getDiscrepancyType())) {
            queryWrapper.eq("discrepancy_type", dto.getDiscrepancyType());
        }
        if (StringUtils.hasText(dto.getTradeType())) {
            queryWrapper.eq("trade_type", dto.getTradeType());
        }
        queryWrapper.orderByDesc("trade_time");

        Page<ReconciliationDetail> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        return detailMapper.selectPage(page, queryWrapper);
    }

    @Override
    public IPage<ReconciliationDiscrepancy> pageDiscrepancies(ReconciliationDiscrepancyQueryDTO dto) {
        if (dto.getBatchNo() == null || dto.getBatchNo().trim().isEmpty()) {
            throw new BizException("批次号不能为空");
        }

        QueryWrapper<ReconciliationDiscrepancy> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("batch_no", dto.getBatchNo());
        if (StringUtils.hasText(dto.getStatus())) {
            queryWrapper.eq("status", dto.getStatus());
        }
        if (StringUtils.hasText(dto.getDiscrepancyType())) {
            queryWrapper.eq("discrepancy_type", dto.getDiscrepancyType());
        }
        queryWrapper.orderByDesc("create_time");

        Page<ReconciliationDiscrepancy> page = new Page<>(dto.getPageNum(), dto.getPageSize());
        return discrepancyMapper.selectPage(page, queryWrapper);
    }

    @Override
    @Deprecated
    public ReconciliationBatch createBatch(String channelCode, Long paymentAppId, String billDate) {
        log.info("创建对账批次（旧接口），channelCode={}, paymentAppId={}, billDate={}", channelCode, paymentAppId, billDate);
        validateBillDate(billDate);
        validateChannelCode(channelCode);

        Long appId = paymentAppId;
        if (appId == null) {
            appId = resolveDefaultAppId(channelCode);
        }

        return createBatchInternal(channelCode, appId, billDate);
    }

    @Override
    @Deprecated
    public ReconciliationBatch getByBatchNo(String batchNo) {
        return getByBatchNoOrThrow(batchNo);
    }

    @Override
    @Deprecated
    public List<ReconciliationBatch> listBatches(String channelCode, String status, String billDate) {
        QueryWrapper<ReconciliationBatch> queryWrapper = new QueryWrapper<>();
        if (StringUtils.hasText(channelCode)) {
            queryWrapper.eq("channel_code", channelCode);
        }
        if (StringUtils.hasText(status)) {
            queryWrapper.eq("status", status);
        }
        if (StringUtils.hasText(billDate)) {
            queryWrapper.eq("bill_date", billDate);
        }
        queryWrapper.orderByDesc("create_time");
        return batchMapper.selectList(queryWrapper);
    }

    @Override
    @Deprecated
    @Transactional(rollbackFor = Exception.class)
    public void resolveDiscrepancy(Long discrepancyId, String resolveRemark) {
        resolveDiscrepancyInternal(discrepancyId, resolveRemark);
    }

    @Override
    @Deprecated
    public List<ReconciliationDetail> listDetails(String batchNo, String matchStatus) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            throw new BizException("批次号不能为空");
        }
        if (StringUtils.hasText(matchStatus)) {
            return detailMapper.selectByBatchNoAndMatchStatus(batchNo, matchStatus);
        }
        return detailMapper.selectByBatchNo(batchNo);
    }

    @Override
    @Deprecated
    public List<ReconciliationDiscrepancy> listDiscrepancies(String batchNo, String status) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            throw new BizException("批次号不能为空");
        }
        if (StringUtils.hasText(status)) {
            return discrepancyMapper.selectByBatchNoAndStatus(batchNo, status);
        }
        return discrepancyMapper.selectByBatchNo(batchNo);
    }

    @Transactional(rollbackFor = Exception.class)
    public void doExecuteBatch(String batchNo) {
        ReconciliationBatch batch = getByBatchNoOrThrow(batchNo);

        log.info("开始下载并解析渠道账单，batchNo={}, channelCode={}", batchNo, batch.getChannelCode());
        ChannelReconciliationStrategy strategy = strategyFactory.getStrategy(batch.getChannelCode());
        List<ReconciliationDetail> channelDetails = strategy.downloadAndParseBill(batch);

        detailMapper.deleteByBatchNo(batchNo);
        if (!channelDetails.isEmpty()) {
            detailMapper.batchInsert(channelDetails);
        }
        updateBatchStatus(batchNo, BatchStatus.BILL_DOWNLOADED, null);
        log.info("渠道账单下载解析完成，batchNo={}, 记录数={}", batchNo, channelDetails.size());

        log.info("开始执行对账匹配，batchNo={}", batchNo);
        reconciliationMatchService.executeMatch(batchNo);
        updateBatchStatus(batchNo, BatchStatus.MATCHED, null);
        log.info("对账匹配完成，batchNo={}", batchNo);

        batch = getByBatchNoOrThrow(batchNo);
        if (batch.getDiscrepancyCount() != null && batch.getDiscrepancyCount() > 0) {
            updateBatchStatus(batchNo, BatchStatus.DISCREPANCY_PENDING, null);
            log.info("对账批次存在差异，batchNo={}, 差异数={}", batchNo, batch.getDiscrepancyCount());
        } else {
            updateBatchStatus(batchNo, BatchStatus.COMPLETED, null);
            log.info("对账批次完成，batchNo={}", batchNo);
        }
    }

    private ReconciliationBatch createBatchInternal(String channelCode, Long appId, String billDate) {
        List<String> nonFinalStatuses = Arrays.asList(
                BatchStatus.CREATED.name(),
                BatchStatus.BILL_DOWNLOADED.name(),
                BatchStatus.LOCAL_COLLECTED.name(),
                BatchStatus.MATCHED.name(),
                BatchStatus.DISCREPANCY_PENDING.name()
        );

        QueryWrapper<ReconciliationBatch> queryWrapper = new QueryWrapper<>();
        queryWrapper.eq("channel_code", channelCode)
                .eq("bill_date", billDate)
                .eq("payment_app_id", appId)
                .in("status", nonFinalStatuses);
        ReconciliationBatch existing = batchMapper.selectOne(queryWrapper);
        if (existing != null) {
            log.info("已存在非终态对账批次，直接返回，batchNo={}", existing.getBatchNo());
            return existing;
        }

        String batchNo = "RC" + OrderNoUtils.getNo();
        ReconciliationBatch batch = new ReconciliationBatch();
        batch.setBatchNo(batchNo);
        batch.setChannelCode(channelCode);
        batch.setPaymentAppId(appId);
        batch.setBillDate(billDate);
        batch.setStatus(BatchStatus.CREATED.name());
        batchMapper.insert(batch);

        log.info("对账批次创建成功，batchNo={}", batchNo);
        return batch;
    }

    private void resolveDiscrepancyInternal(Long discrepancyId, String resolveRemark) {
        log.info("处理差异单，discrepancyId={}, resolveRemark={}", discrepancyId, resolveRemark);

        if (discrepancyId == null) {
            throw new BizException("差异单ID不能为空");
        }

        ReconciliationDiscrepancy discrepancy = discrepancyMapper.selectById(discrepancyId);
        if (discrepancy == null) {
            throw new BizException("差异单不存在，discrepancyId=" + discrepancyId);
        }

        if (DiscrepancyStatus.RESOLVED.name().equals(discrepancy.getStatus())
                || DiscrepancyStatus.AUTO_RESOLVED.name().equals(discrepancy.getStatus())) {
            log.info("差异单已处理，跳过，discrepancyId={}", discrepancyId);
            return;
        }

        ReconciliationDiscrepancy update = new ReconciliationDiscrepancy();
        update.setStatus(DiscrepancyStatus.RESOLVED.name());
        update.setResolveRemark((resolveRemark == null || resolveRemark.trim().isEmpty())
                ? "已处理" : resolveRemark.trim());
        update.setResolvedTime(new Date());
        update.setResolvedBy("system");
        discrepancyMapper.update(update, new QueryWrapper<ReconciliationDiscrepancy>()
                .eq("id", discrepancyId));

        log.info("差异单处理完成，discrepancyId={}", discrepancyId);

        checkAndAdvanceBatchStatus(discrepancy.getBatchNo());
    }

    private void validateBillDate(String billDate) {
        if (billDate == null || billDate.trim().isEmpty()) {
            throw new BizException("账单日期不能为空");
        }
        try {
            LocalDate.parse(billDate);
        } catch (DateTimeParseException e) {
            throw new BizException("账单日期格式必须为yyyy-MM-dd");
        }
    }

    private void validateChannelCode(String channelCode) {
        if (channelCode == null || channelCode.trim().isEmpty()) {
            throw new BizException("渠道编码不能为空");
        }
        if (!PaymentConfigLoader.CHANNEL_WXPAY.equals(channelCode)
                && !PaymentConfigLoader.CHANNEL_ALIPAY.equals(channelCode)) {
            throw new BizException("不支持的渠道编码：" + channelCode);
        }
    }

    private Long resolveDefaultAppId(String channelCode) {
        cc.ivera.config.PaymentAppConfig config =
                paymentConfigLoader.getDefaultAppConfigByChannelCode(channelCode);
        if (config == null) {
            throw new BizException("未配置默认支付应用，channelCode=" + channelCode);
        }
        return config.getAppId();
    }

    private ReconciliationBatch getByBatchNoOrThrow(String batchNo) {
        if (batchNo == null || batchNo.trim().isEmpty()) {
            throw new BizException("批次号不能为空");
        }
        ReconciliationBatch batch = batchMapper.selectByBatchNo(batchNo);
        if (batch == null) {
            throw new BizException("对账批次不存在，batchNo=" + batchNo);
        }
        return batch;
    }

    private void updateBatchStatus(String batchNo, BatchStatus status, String failureReason) {
        ReconciliationBatch update = new ReconciliationBatch();
        update.setStatus(status.name());
        if (failureReason != null) {
            update.setFailureReason(failureReason);
        }
        batchMapper.update(update, new QueryWrapper<ReconciliationBatch>()
                .eq("batch_no", batchNo));
    }

    private void checkAndAdvanceBatchStatus(String batchNo) {
        QueryWrapper<ReconciliationDiscrepancy> wrapper = new QueryWrapper<>();
        wrapper.eq("batch_no", batchNo)
                .eq("status", DiscrepancyStatus.OPEN.name());
        Integer openCount = discrepancyMapper.selectCount(wrapper);
        if (openCount == null || openCount == 0) {
            log.info("批次所有差异单已处理，推进批次状态，batchNo={}", batchNo);
            updateBatchStatus(batchNo, BatchStatus.RESOLVED, null);
            updateBatchStatus(batchNo, BatchStatus.COMPLETED, null);
        }
    }

    private ReconciliationBatchVO convertToBatchVO(ReconciliationBatch batch) {
        ReconciliationBatchVO vo = new ReconciliationBatchVO();
        vo.setId(batch.getId());
        vo.setBatchNo(batch.getBatchNo());
        vo.setChannelCode(batch.getChannelCode());
        vo.setPaymentAppId(batch.getPaymentAppId());
        vo.setBillDate(batch.getBillDate());
        vo.setStatus(batch.getStatus());
        vo.setChannelTotalCount(batch.getChannelTotalCount());
        vo.setChannelTotalAmount(batch.getChannelTotalAmount());
        vo.setLocalTotalCount(batch.getLocalTotalCount());
        vo.setLocalTotalAmount(batch.getLocalTotalAmount());
        vo.setMatchedCount(batch.getMatchedCount());
        vo.setMatchedAmount(batch.getMatchedAmount());
        vo.setDiscrepancyCount(batch.getDiscrepancyCount());
        vo.setOverpaymentCount(batch.getOverpaymentCount());
        vo.setUnderpaymentCount(batch.getUnderpaymentCount());
        vo.setAmountMismatchCount(batch.getAmountMismatchCount());
        vo.setStatusMismatchCount(batch.getStatusMismatchCount());
        vo.setFailureReason(batch.getFailureReason());
        vo.setCreateTime(batch.getCreateTime());
        vo.setUpdateTime(batch.getUpdateTime());
        return vo;
    }
}
