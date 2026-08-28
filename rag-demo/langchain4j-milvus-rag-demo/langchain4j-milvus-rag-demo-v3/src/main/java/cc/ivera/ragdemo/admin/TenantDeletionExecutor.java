package cc.ivera.ragdemo.admin;


import cc.ivera.ragdemo.config.RagProperties;
import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionStage;
import cc.ivera.ragdemo.domain.tenant.TenantDataDeletionTask;
import cc.ivera.ragdemo.mapper.TenantDataDeletionStageMapper;
import cc.ivera.ragdemo.mapper.TenantDataDeletionTaskMapper;
import cc.ivera.ragdemo.service.ragops.TenantDeletionStateMachine;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class TenantDeletionExecutor {

    private final RagProperties properties;
    private final TenantDataDeletionTaskMapper taskMapper;
    private final TenantDataDeletionStageMapper stageMapper;
    private final List<TenantDeletionWorker> workers;
    private final TenantDeletionStateMachine stateMachine = new TenantDeletionStateMachine();

    @Transactional
    public Object run(Long taskId, String requestedMode) {
        TenantDeletionExecutionMode mode = TenantDeletionExecutionMode.normalize(requestedMode);
        stateMachine.assertExecutionAllowed(mode, properties.getTenantDeletion().isExecuteEnabled());
        TenantDataDeletionTask task = requiredTask(taskId);
        acquireLock(task, mode);
        boolean failed = false;
        Map<String, String> verify = new LinkedHashMap<>();
        for (String stageCode : TenantDeletionStageCode.defaultStageCodes()) {
            if (isCancelRequested(task.getId())) {
                markTask(task, TenantDeletionTaskStatus.CANCELLED, null);
                return detail(task.getId());
            }
            TenantDeletionWorker worker = worker(stageCode);
            TenantDataDeletionStage stage = ensureStage(task.getId(), stageCode);
            markStageRunning(stage);
            TenantDeletionStageResult result = runStage(worker, task, mode);
            updateStage(stage, result, mode);
            if ("FAILED".equals(result.status())) {
                failed = true;
            }
        }
        markTask(task, TenantDeletionTaskStatus.VERIFYING, null);
        for (String stageCode : TenantDeletionStageCode.defaultStageCodes()) {
            TenantDeletionWorker worker = worker(stageCode);
            TenantDataDeletionStage stage = ensureStage(task.getId(), stageCode);
            TenantDeletionStageResult result = safeVerify(worker, task);
            stage.setVerifyStatus(result.status());
            stage.setVerifyResultJson(result.detailJson());
            if ("FAILED".equals(result.status())) {
                failed = true;
                verify.put(stageCode, result.message());
            }
            stage.setUpdatedAt(LocalDateTime.now());
            stageMapper.updateById(stage);
        }
        task = requiredTask(task.getId());
        task.setVerifyResultJson(toJson(verify));
        markTask(task, failed ? TenantDeletionTaskStatus.PARTIAL_FAILED : TenantDeletionTaskStatus.SUCCEEDED, failed ? "One or more tenant deletion stages failed" : null);
        return detail(task.getId());
    }

    @Transactional
    public Object cancel(Long taskId) {
        TenantDataDeletionTask task = requiredTask(taskId);
        TenantDeletionTaskStatus status = status(task.getTaskStatus());
        if (status == TenantDeletionTaskStatus.RUNNING || status == TenantDeletionTaskStatus.VERIFYING) {
            stateMachine.assertTransition(status, TenantDeletionTaskStatus.CANCEL_REQUESTED);
            task.setTaskStatus(TenantDeletionTaskStatus.CANCEL_REQUESTED.name());
        } else if (status == TenantDeletionTaskStatus.PENDING) {
            task.setTaskStatus(TenantDeletionTaskStatus.CANCELLED.name());
            task.setFinishedAt(LocalDateTime.now());
        }
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
        return detail(taskId);
    }

    @Transactional
    public Object retryStage(Long taskId, String stageCode, String requestedMode) {
        TenantDeletionExecutionMode mode = TenantDeletionExecutionMode.normalize(requestedMode);
        stateMachine.assertExecutionAllowed(mode, properties.getTenantDeletion().isExecuteEnabled());
        TenantDataDeletionTask task = requiredTask(taskId);
        acquireLock(task, mode);
        String normalized = stageCode == null ? "" : stageCode.trim().toUpperCase();
        TenantDeletionWorker worker = worker(normalized);
        TenantDataDeletionStage stage = ensureStage(taskId, normalized);
        markStageRunning(stage);
        TenantDeletionStageResult result = runStage(worker, task, mode);
        updateStage(stage, result, mode);
        TenantDeletionStageResult verify = safeVerify(worker, task);
        stage = ensureStage(taskId, normalized);
        stage.setVerifyStatus(verify.status());
        stage.setVerifyResultJson(verify.detailJson());
        stage.setUpdatedAt(LocalDateTime.now());
        stageMapper.updateById(stage);
        markTask(requiredTask(taskId),
                "FAILED".equals(result.status()) || "FAILED".equals(verify.status())
                        ? TenantDeletionTaskStatus.PARTIAL_FAILED
                        : TenantDeletionTaskStatus.SUCCEEDED,
                null);
        return detail(taskId);
    }

    public Object detail(Long taskId) {
        TenantDataDeletionTask task = requiredTask(taskId);
        List<TenantDataDeletionStage> stages = stageMapper.selectList(new LambdaQueryWrapper<TenantDataDeletionStage>()
                .eq(TenantDataDeletionStage::getTaskId, taskId)
                .orderByAsc(TenantDataDeletionStage::getId));
        return Map.of("task", task, "stages", stages);
    }

    private TenantDeletionStageResult runStage(TenantDeletionWorker worker,
                                               TenantDataDeletionTask task,
                                               TenantDeletionExecutionMode mode) {
        try {
            return mode == TenantDeletionExecutionMode.DRY_RUN ? worker.dryRun(task) : worker.execute(task);
        } catch (Exception ex) {
            return TenantDeletionStageResult.failed(worker.stageCode(), ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private TenantDeletionStageResult safeVerify(TenantDeletionWorker worker, TenantDataDeletionTask task) {
        try {
            return worker.verify(task);
        } catch (Exception ex) {
            return TenantDeletionStageResult.failed(worker.stageCode(), ex.getClass().getSimpleName(), ex.getMessage());
        }
    }

    private void acquireLock(TenantDataDeletionTask task, TenantDeletionExecutionMode mode) {
        TenantDeletionTaskStatus current = status(task.getTaskStatus());
        if (current != TenantDeletionTaskStatus.PENDING
                && current != TenantDeletionTaskStatus.FAILED
                && current != TenantDeletionTaskStatus.PARTIAL_FAILED
                && current != TenantDeletionTaskStatus.CANCELLED) {
            throw new IllegalStateException("Tenant deletion task is already active: " + current);
        }
        task.setExecutionMode(mode.name());
        task.setTaskStatus(TenantDeletionTaskStatus.RUNNING.name());
        task.setLockOwner("node-" + UUID.randomUUID().toString().replace("-", ""));
        task.setLockUntil(LocalDateTime.now().plusSeconds(properties.getTenantDeletion().getLockTtlSeconds()));
        task.setStartedAt(task.getStartedAt() == null ? LocalDateTime.now() : task.getStartedAt());
        task.setFinishedAt(null);
        task.setErrorMessage(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private boolean isCancelRequested(Long taskId) {
        TenantDataDeletionTask task = requiredTask(taskId);
        return TenantDeletionTaskStatus.CANCEL_REQUESTED.name().equals(task.getTaskStatus());
    }

    private void markTask(TenantDataDeletionTask task, TenantDeletionTaskStatus status, String error) {
        task.setTaskStatus(status.name());
        task.setErrorMessage(error);
        task.setFinishedAt(status == TenantDeletionTaskStatus.SUCCEEDED
                || status == TenantDeletionTaskStatus.FAILED
                || status == TenantDeletionTaskStatus.PARTIAL_FAILED
                || status == TenantDeletionTaskStatus.CANCELLED ? LocalDateTime.now() : task.getFinishedAt());
        task.setLockOwner(null);
        task.setLockUntil(null);
        task.setUpdatedAt(LocalDateTime.now());
        taskMapper.updateById(task);
    }

    private void markStageRunning(TenantDataDeletionStage stage) {
        stage.setStageStatus("RUNNING");
        stage.setStartedAt(stage.getStartedAt() == null ? LocalDateTime.now() : stage.getStartedAt());
        stage.setFinishedAt(null);
        stage.setErrorCode(null);
        stage.setErrorMessage(null);
        stage.setUpdatedAt(LocalDateTime.now());
        stageMapper.updateById(stage);
    }

    private void updateStage(TenantDataDeletionStage stage,
                             TenantDeletionStageResult result,
                             TenantDeletionExecutionMode mode) {
        stage.setStageStatus(result.status());
        stage.setDeletedCount(result.affectedCount());
        stage.setErrorCode(result.errorCode());
        stage.setErrorMessage(result.message());
        if (mode == TenantDeletionExecutionMode.DRY_RUN) {
            stage.setDryRunResultJson(result.detailJson());
        }
        stage.setFinishedAt(LocalDateTime.now());
        stage.setUpdatedAt(LocalDateTime.now());
        stageMapper.updateById(stage);
    }

    private TenantDataDeletionTask requiredTask(Long taskId) {
        TenantDataDeletionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Tenant deletion task not found: " + taskId);
        }
        return task;
    }

    private TenantDataDeletionStage ensureStage(Long taskId, String stageCode) {
        TenantDataDeletionStage stage = stageMapper.selectOne(new LambdaQueryWrapper<TenantDataDeletionStage>()
                .eq(TenantDataDeletionStage::getTaskId, taskId)
                .eq(TenantDataDeletionStage::getStageCode, stageCode)
                .last("LIMIT 1"));
        if (stage != null) {
            return stage;
        }
        TenantDataDeletionStage created = new TenantDataDeletionStage();
        created.setTaskId(taskId);
        created.setStageCode(stageCode);
        created.setStageStatus("PENDING");
        created.setDeletedCount(0L);
        created.setCreatedAt(LocalDateTime.now());
        created.setUpdatedAt(LocalDateTime.now());
        stageMapper.insert(created);
        return created;
    }

    private TenantDeletionWorker worker(String stageCode) {
        return workers.stream()
                .filter(worker -> worker.stageCode().equals(stageCode))
                .findFirst()
                .orElseThrow(() -> new IllegalArgumentException("Unsupported tenant deletion stage: " + stageCode));
    }

    private TenantDeletionTaskStatus status(String status) {
        if (status == null || status.isBlank()) {
            return TenantDeletionTaskStatus.PENDING;
        }
        return TenantDeletionTaskStatus.valueOf(status.trim().toUpperCase());
    }

    private String toJson(Map<String, String> values) {
        if (values == null || values.isEmpty()) {
            return "{}";
        }
        StringBuilder json = new StringBuilder("{");
        int index = 0;
        for (Map.Entry<String, String> entry : values.entrySet()) {
            if (index++ > 0) {
                json.append(',');
            }
            json.append('"').append(entry.getKey()).append("\":\"")
                    .append(entry.getValue() == null ? "" : entry.getValue().replace("\\", "\\\\").replace("\"", "\\\""))
                    .append('"');
        }
        json.append('}');
        return json.toString();
    }
}
