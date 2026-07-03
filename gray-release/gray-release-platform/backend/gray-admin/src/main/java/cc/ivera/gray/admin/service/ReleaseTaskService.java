package cc.ivera.gray.admin.service;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import cc.ivera.gray.admin.entity.ReleaseTask;
import cc.ivera.gray.admin.mapper.ReleaseTaskMapper;
import cc.ivera.gray.common.GrayEnums.AlertLevel;
import cc.ivera.gray.common.GrayEnums.ReleaseStatus;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class ReleaseTaskService {
    private final ReleaseTaskMapper releaseTaskMapper;
    private final AuditService auditService;
    private final ReleaseApprovalService releaseApprovalService;
    private final AlertService alertService;
    private final ServicePolicyService servicePolicyService;
    private final ObjectMapper objectMapper;

    public ReleaseTaskService(ReleaseTaskMapper releaseTaskMapper,
                              AuditService auditService,
                              ReleaseApprovalService releaseApprovalService,
                              AlertService alertService,
                              ServicePolicyService servicePolicyService,
                              ObjectMapper objectMapper) {
        this.releaseTaskMapper = releaseTaskMapper;
        this.auditService = auditService;
        this.releaseApprovalService = releaseApprovalService;
        this.alertService = alertService;
        this.servicePolicyService = servicePolicyService;
        this.objectMapper = objectMapper;
    }

    public List<ReleaseTask> list(String serviceId) {
        LambdaQueryWrapper<ReleaseTask> wrapper = new LambdaQueryWrapper<ReleaseTask>()
                .orderByDesc(ReleaseTask::getUpdateTime);
        if (serviceId != null && !serviceId.isBlank()) {
            wrapper.eq(ReleaseTask::getServiceId, serviceId);
        }
        return releaseTaskMapper.selectList(wrapper);
    }

    @Transactional
    public ReleaseTask create(ReleaseTask task, String operator) {
        if (task.getCurrentPercent() == null) {
            task.setCurrentPercent(0);
        }
        task.setStatus(ReleaseStatus.WAITING_APPROVAL.name());
        if (task.getStagesJson() == null || task.getStagesJson().isBlank()) {
            task.setStagesJson("[1,5,20,50,100]");
        }
        if (task.getAutoRollbackEnabled() == null) {
            task.setAutoRollbackEnabled(true);
        }
        if (task.getErrorRateThreshold() == null) {
            task.setErrorRateThreshold(0.05);
        }
        if (task.getP99LatencyThresholdMs() == null) {
            task.setP99LatencyThresholdMs(1200);
        }
        task.setLatestErrorRate(0.0);
        task.setLatestP99LatencyMs(0);
        task.setHealthStatus("UNKNOWN");
        releaseTaskMapper.insert(task);
        releaseApprovalService.createPending(task.getId(), operator == null || operator.isBlank() ? task.getOwner() : operator);
        auditService.record(operator, "CREATE", "RELEASE_TASK", String.valueOf(task.getId()), null, task.getTaskName());
        return task;
    }

    @Transactional
    public ReleaseTask changeStatus(Long id, ReleaseStatus status, String reason, String operator) {
        ReleaseTask task = requireTask(id);
        if (status == ReleaseStatus.RUNNING && !releaseApprovalService.hasApproved(id)) {
            throw new IllegalArgumentException("发布任务未审批通过，不能启动");
        }
        String before = task.getStatus();
        task.setStatus(status.name());
        if (status == ReleaseStatus.ROLLED_BACK) {
            task.setCurrentPercent(0);
            task.setRollbackReason(reason);
        }
        if (status == ReleaseStatus.COMPLETED) {
            task.setCurrentPercent(100);
            if ("BLUE_GREEN".equals(task.getStrategy())) {
                servicePolicyService.blueGreenSwitch(task.getServiceId(), "green", operator);
            }
        }
        releaseTaskMapper.updateById(task);
        auditService.record(operator, status.name(), "RELEASE_TASK", String.valueOf(id), before, task.getStatus());
        return releaseTaskMapper.selectById(id);
    }

    @Transactional
    public ReleaseTask advance(Long id, Integer percent, String operator) {
        ReleaseTask task = requireTask(id);
        if (!ReleaseStatus.RUNNING.name().equals(task.getStatus())) {
            throw new IllegalArgumentException("只有运行中的发布任务可以推进");
        }
        int next = Math.max(0, Math.min(100, percent));
        String before = task.getCurrentPercent() + "%";
        task.setCurrentPercent(next);
        task.setStatus(next >= 100 ? ReleaseStatus.COMPLETED.name() : ReleaseStatus.RUNNING.name());
        releaseTaskMapper.updateById(task);
        auditService.record(operator, "ADVANCE", "RELEASE_TASK", String.valueOf(id), before, next + "%");
        return releaseTaskMapper.selectById(id);
    }

    @Transactional
    public ReleaseTask reportMetrics(Long id, Double errorRate, Integer p99LatencyMs, String operator) {
        ReleaseTask task = requireTask(id);
        double nextErrorRate = errorRate == null ? 0.0 : errorRate;
        int nextP99 = p99LatencyMs == null ? 0 : p99LatencyMs;
        task.setLatestErrorRate(nextErrorRate);
        task.setLatestP99LatencyMs(nextP99);

        double threshold = task.getErrorRateThreshold() == null ? 0.05 : task.getErrorRateThreshold();
        int latencyThreshold = task.getP99LatencyThresholdMs() == null ? 1200 : task.getP99LatencyThresholdMs();
        boolean unhealthy = nextErrorRate > threshold || nextP99 > latencyThreshold;
        task.setHealthStatus(unhealthy ? "UNHEALTHY" : "HEALTHY");
        if (Boolean.TRUE.equals(task.getAutoRollbackEnabled())
                && ReleaseStatus.RUNNING.name().equals(task.getStatus())
                && unhealthy) {
            task.setStatus(ReleaseStatus.ROLLED_BACK.name());
            task.setCurrentPercent(0);
            task.setRollbackReason("自动回滚：errorRate=" + nextErrorRate + ", p99=" + nextP99 + "ms");
            auditService.record(operator, "AUTO_ROLLBACK", "RELEASE_TASK", String.valueOf(id), "RUNNING", task.getRollbackReason());
            alertService.create(AlertLevel.CRITICAL, "release-task-" + id, "发布任务自动回滚", task.getRollbackReason());
        }
        releaseTaskMapper.updateById(task);
        return releaseTaskMapper.selectById(id);
    }

    @Scheduled(fixedDelayString = "${gray.release.auto-advance-delay-ms:30000}")
    public void autoAdvanceHealthyTasks() {
        List<ReleaseTask> tasks = releaseTaskMapper.selectList(new LambdaQueryWrapper<ReleaseTask>()
                .eq(ReleaseTask::getStatus, ReleaseStatus.RUNNING.name())
                .eq(ReleaseTask::getHealthStatus, "HEALTHY"));
        for (ReleaseTask task : tasks) {
            try {
                Integer next = nextStage(task);
                if (next != null && next > task.getCurrentPercent()) {
                    advance(task.getId(), next, "auto-advance");
                    alertService.create(AlertLevel.INFO, "release-task-" + task.getId(),
                            "发布阶段自动推进", task.getTaskName() + " 自动推进到 " + next + "%");
                }
            } catch (Exception ignored) {
                // 单个任务推进失败不影响其他任务。
            }
        }
    }

    private Integer nextStage(ReleaseTask task) throws Exception {
        List<Integer> stages = objectMapper.readValue(task.getStagesJson(), new TypeReference<>() {
        });
        for (Integer stage : stages) {
            if (stage > task.getCurrentPercent()) {
                return stage;
            }
        }
        return null;
    }

    private ReleaseTask requireTask(Long id) {
        ReleaseTask task = releaseTaskMapper.selectById(id);
        if (task == null) {
            throw new IllegalArgumentException("发布任务不存在");
        }
        return task;
    }
}
