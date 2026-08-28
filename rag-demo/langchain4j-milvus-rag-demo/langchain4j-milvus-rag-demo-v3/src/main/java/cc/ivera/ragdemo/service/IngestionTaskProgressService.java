package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.domain.rag.*;
import cc.ivera.ragdemo.mapper.RagIngestionTaskEventMapper;
import cc.ivera.ragdemo.mapper.RagIngestionTaskMapper;
import cc.ivera.ragdemo.mapper.RagIngestionTaskShardMapper;
import cc.ivera.ragdemo.mapper.RagIngestionTaskStageMapper;
import cc.ivera.ragdemo.model.knowledge.IngestionShardSummary;
import cc.ivera.ragdemo.model.knowledge.IngestionTaskEventView;
import cc.ivera.ragdemo.model.knowledge.IngestionTaskProgressSnapshot;
import cc.ivera.ragdemo.model.query.PageQuery;
import cc.ivera.ragdemo.model.query.PageResponse;
import cc.ivera.ragdemo.service.ragops.IngestionStageProgressPolicy;
import cc.ivera.ragdemo.service.ragops.IngestionStageProgressPolicy.StagePlan;
import cc.ivera.ragdemo.tenant.TenantContextHolder;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class IngestionTaskProgressService {

    private final RagIngestionTaskMapper taskMapper;
    private final RagIngestionTaskStageMapper stageMapper;
    private final RagIngestionTaskShardMapper shardMapper;
    private final RagIngestionTaskEventMapper eventMapper;
    private final IngestionTaskEventPublisher eventPublisher;

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void initializeStages(RagIngestionTask task, List<StagePlan> stages) {
        if (task == null || task.getId() == null) {
            return;
        }
        LocalDateTime now = LocalDateTime.now();
        for (StagePlan plan : stages) {
            RagIngestionTaskStage existing = findStage(task.getId(), plan.stageCode());
            if (existing != null) {
                continue;
            }
            RagIngestionTaskStage stage = new RagIngestionTaskStage();
            stage.setTaskId(task.getId());
            stage.setStageCode(plan.stageCode());
            stage.setStageName(plan.stageName());
            stage.setStageOrder(plan.stageOrder());
            stage.setStageWeight(plan.weight());
            stage.setStageStatus(IngestionStageStatus.PENDING.name());
            stage.setProgress(0);
            stage.setTotalCount(0);
            stage.setSuccessCount(0);
            stage.setFailedCount(0);
            stage.setCreatedAt(now);
            stage.setUpdatedAt(now);
            stageMapper.insert(stage);
        }
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startTask(Long taskId) {
        emitEvent(taskId, "TASK_STARTED", null, null, "Ingestion task started", null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void startStage(Long taskId, String stageCode) {
        RagIngestionTaskStage stage = upsertStage(taskId, stageCode);
        LocalDateTime now = LocalDateTime.now();
        stage.setStageStatus(IngestionStageStatus.RUNNING.name());
        stage.setProgress(0);
        stage.setStartedAt(stage.getStartedAt() == null ? now : stage.getStartedAt());
        stage.setFinishedAt(null);
        stage.setErrorCode(null);
        stage.setErrorMessage(null);
        stage.setUpdatedAt(now);
        stageMapper.updateById(stage);
        updateTaskProgress(taskId, stageCode, 0);
        emitEvent(taskId, "STAGE_STARTED", stageCode, null, stage.getStageName(), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void updateStageProgress(Long taskId, String stageCode, int successCount, int failedCount, int totalCount) {
        RagIngestionTaskStage stage = upsertStage(taskId, stageCode);
        int progress = IngestionStageProgressPolicy.unitProgress(successCount, failedCount, totalCount);
        LocalDateTime now = LocalDateTime.now();
        stage.setStageStatus(IngestionStageStatus.RUNNING.name());
        stage.setProgress(progress);
        stage.setTotalCount(Math.max(0, totalCount));
        stage.setSuccessCount(Math.max(0, successCount));
        stage.setFailedCount(Math.max(0, failedCount));
        stage.setStartedAt(stage.getStartedAt() == null ? now : stage.getStartedAt());
        stage.setUpdatedAt(now);
        stageMapper.updateById(stage);
        int taskProgress = updateTaskProgress(taskId, stageCode, progress);
        emitEvent(taskId, "STAGE_PROGRESS", stageCode, null, "Stage progress updated",
                "{\"successCount\":" + Math.max(0, successCount)
                        + ",\"failedCount\":" + Math.max(0, failedCount)
                        + ",\"totalCount\":" + Math.max(0, totalCount)
                        + ",\"taskProgress\":" + taskProgress + "}");
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void completeStage(Long taskId, String stageCode, int successCount, int failedCount, int totalCount) {
        RagIngestionTaskStage stage = upsertStage(taskId, stageCode);
        LocalDateTime now = LocalDateTime.now();
        stage.setStageStatus(IngestionStageStatus.SUCCESS.name());
        stage.setProgress(100);
        stage.setTotalCount(Math.max(0, totalCount));
        stage.setSuccessCount(Math.max(0, successCount));
        stage.setFailedCount(Math.max(0, failedCount));
        stage.setStartedAt(stage.getStartedAt() == null ? now : stage.getStartedAt());
        stage.setFinishedAt(now);
        stage.setUpdatedAt(now);
        stageMapper.updateById(stage);
        updateTaskProgress(taskId, stageCode, 100);
        emitEvent(taskId, "STAGE_COMPLETED", stageCode, null, stage.getStageName(), null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void skipStage(Long taskId, String stageCode, String reason) {
        RagIngestionTaskStage stage = upsertStage(taskId, stageCode);
        LocalDateTime now = LocalDateTime.now();
        stage.setStageStatus(IngestionStageStatus.SKIPPED.name());
        stage.setProgress(100);
        stage.setStartedAt(stage.getStartedAt() == null ? now : stage.getStartedAt());
        stage.setFinishedAt(now);
        stage.setMetadataJson(StringUtils.hasText(reason) ? "{\"reason\":\"" + jsonEscape(reason) + "\"}" : null);
        stage.setUpdatedAt(now);
        stageMapper.updateById(stage);
        updateTaskProgress(taskId, stageCode, 100);
        emitEvent(taskId, "STAGE_SKIPPED", stageCode, null, reason, null);
    }

    @Transactional(propagation = Propagation.REQUIRES_NEW)
    public void failStage(Long taskId, String stageCode, String errorCode, String errorMessage) {
        RagIngestionTaskStage stage = upsertStage(taskId, stageCode);
        LocalDateTime now = LocalDateTime.now();
        stage.setStageStatus(IngestionStageStatus.FAILED.name());
        stage.setErrorCode(errorCode);
        stage.setErrorMessage(truncate(errorMessage, 1900));
        stage.setFinishedAt(now);
        stage.setUpdatedAt(now);
        stageMapper.updateById(stage);
        emitEvent(taskId, "STAGE_FAILED", stageCode, null, truncate(errorMessage, 900), null);
    }

    @Transactional
    public void cancelRunningStages(Long taskId) {
        List<RagIngestionTaskStage> runningStages = stageMapper.selectList(new LambdaQueryWrapper<RagIngestionTaskStage>()
                .eq(RagIngestionTaskStage::getTaskId, taskId)
                .eq(RagIngestionTaskStage::getStageStatus, IngestionStageStatus.RUNNING.name()));
        LocalDateTime now = LocalDateTime.now();
        for (RagIngestionTaskStage stage : runningStages) {
            stage.setStageStatus(IngestionStageStatus.CANCELLED.name());
            stage.setFinishedAt(now);
            stage.setUpdatedAt(now);
            stageMapper.updateById(stage);
        }
        emitEvent(taskId, "TASK_CANCELLED", null, null, "Ingestion task cancelled", null);
    }

    @Transactional
    public void emitTaskTerminalEvent(Long taskId, String eventType, String message) {
        emitEvent(taskId, eventType, null, null, message, null);
    }

    public IngestionTaskProgressSnapshot snapshot(Long taskId) {
        RagIngestionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Ingestion task not found: " + taskId);
        }
        List<RagIngestionTaskStage> stages = listStages(taskId);
        return new IngestionTaskProgressSnapshot(
                task.getId(),
                task.getTaskStatus(),
                task.getProgress(),
                task.getCurrentStage(),
                task.getStageProgress(),
                task.getCancelRequested(),
                task.getLastEventId(),
                task,
                stages,
                shardSummary(taskId)
        );
    }

    public List<RagIngestionTaskStage> listStages(Long taskId) {
        return stageMapper.selectList(new LambdaQueryWrapper<RagIngestionTaskStage>()
                .eq(RagIngestionTaskStage::getTaskId, taskId)
                .orderByAsc(RagIngestionTaskStage::getStageOrder));
    }

    public PageResponse<IngestionTaskEventView> pageEvents(Long taskId, Long afterEventId, PageQuery pageQuery) {
        PageQuery query = normalizePageQuery(pageQuery, "id");
        LambdaQueryWrapper<RagIngestionTaskEvent> countWrapper = eventQuery(taskId, afterEventId);
        long total = eventMapper.selectCount(countWrapper);
        LambdaQueryWrapper<RagIngestionTaskEvent> rowsQuery = eventQuery(taskId, afterEventId);
        rowsQuery.orderBy(true, query.ascending(), RagIngestionTaskEvent::getId);
        rowsQuery.last("LIMIT " + query.offset(total) + ", " + query.effectivePageSize(total));
        List<IngestionTaskEventView> rows = eventMapper.selectList(rowsQuery).stream()
                .map(IngestionTaskEventPublisher::toView)
                .toList();
        return PageResponse.of(query, total, rows);
    }

    public PageResponse<RagIngestionTaskShard> pageShards(Long taskId,
                                                          String stageCode,
                                                          String shardStatus,
                                                          PageQuery pageQuery) {
        PageQuery query = normalizePageQuery(pageQuery, "id");
        LambdaQueryWrapper<RagIngestionTaskShard> countWrapper = shardQuery(taskId, stageCode, shardStatus);
        long total = shardMapper.selectCount(countWrapper);
        LambdaQueryWrapper<RagIngestionTaskShard> rowsQuery = shardQuery(taskId, stageCode, shardStatus);
        rowsQuery.orderBy(true, query.ascending(), RagIngestionTaskShard::getId);
        rowsQuery.last("LIMIT " + query.offset(total) + ", " + query.effectivePageSize(total));
        return PageResponse.of(query, total, shardMapper.selectList(rowsQuery));
    }

    public IngestionShardSummary shardSummary(Long taskId) {
        List<RagIngestionTaskShard> shards = shardMapper.selectList(new LambdaQueryWrapper<RagIngestionTaskShard>()
                .eq(RagIngestionTaskShard::getTaskId, taskId));
        long pending = countStatus(shards, IngestionShardStatus.PENDING);
        long running = countStatus(shards, IngestionShardStatus.RUNNING);
        long success = countStatus(shards, IngestionShardStatus.SUCCESS);
        long failedRetryable = countStatus(shards, IngestionShardStatus.FAILED_RETRYABLE);
        long failedFinal = countStatus(shards, IngestionShardStatus.FAILED_FINAL);
        long cancelled = countStatus(shards, IngestionShardStatus.CANCELLED);
        return new IngestionShardSummary(shards.size(), pending, running, success, failedRetryable, failedFinal, cancelled);
    }

    private void emitEvent(Long taskId, String eventType, String stageCode, String shardKey, String message, String payloadJson) {
        if (taskId == null) {
            return;
        }
        RagIngestionTask task = taskMapper.selectById(taskId);
        RagIngestionTaskEvent event = new RagIngestionTaskEvent();
        event.setTaskId(taskId);
        event.setEventType(eventType);
        event.setStageCode(stageCode);
        event.setShardKey(shardKey);
        event.setProgress(task == null ? null : task.getProgress());
        event.setStageProgress(task == null ? null : task.getStageProgress());
        event.setMessage(truncate(message, 1000));
        event.setPayloadJson(payloadJson);
        event.setCreatedAt(LocalDateTime.now());
        eventMapper.insert(event);

        if (task != null && event.getId() != null) {
            task.setLastEventId(event.getId());
            task.setHeartbeatAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        }
        eventPublisher.publish(task == null ? TenantContextHolder.currentTenantId().orElse(null) : task.getTenantId(), event);
    }

    private int updateTaskProgress(Long taskId, String currentStage, int stageProgress) {
        List<RagIngestionTaskStage> stages = listStages(taskId);
        Map<String, Integer> progressByStage = new LinkedHashMap<>();
        for (RagIngestionTaskStage stage : stages) {
            progressByStage.put(stage.getStageCode(), stage.getProgress());
        }
        int taskProgress = IngestionStageProgressPolicy.weightedProgress(IngestionStageProgressPolicy.defaultStages(), progressByStage);
        RagIngestionTask task = taskMapper.selectById(taskId);
        if (task != null) {
            task.setCurrentStage(currentStage);
            task.setStageProgress(IngestionStageProgressPolicy.clampProgress(stageProgress));
            task.setProgress(taskProgress);
            task.setHeartbeatAt(LocalDateTime.now());
            task.setUpdatedAt(LocalDateTime.now());
            taskMapper.updateById(task);
        }
        return taskProgress;
    }

    private RagIngestionTaskStage upsertStage(Long taskId, String stageCode) {
        RagIngestionTaskStage existing = findStage(taskId, stageCode);
        if (existing != null) {
            return existing;
        }
        StagePlan plan = IngestionStageProgressPolicy.defaultStageMap().get(stageCode);
        LocalDateTime now = LocalDateTime.now();
        RagIngestionTaskStage stage = new RagIngestionTaskStage();
        stage.setTaskId(taskId);
        stage.setStageCode(stageCode);
        stage.setStageName(plan == null ? stageCode : plan.stageName());
        stage.setStageOrder(plan == null ? 999 : plan.stageOrder());
        stage.setStageWeight(plan == null ? 0 : plan.weight());
        stage.setStageStatus(IngestionStageStatus.PENDING.name());
        stage.setProgress(0);
        stage.setTotalCount(0);
        stage.setSuccessCount(0);
        stage.setFailedCount(0);
        stage.setCreatedAt(now);
        stage.setUpdatedAt(now);
        stageMapper.insert(stage);
        return stage;
    }

    private RagIngestionTaskStage findStage(Long taskId, String stageCode) {
        return stageMapper.selectOne(new LambdaQueryWrapper<RagIngestionTaskStage>()
                .eq(RagIngestionTaskStage::getTaskId, taskId)
                .eq(RagIngestionTaskStage::getStageCode, stageCode)
                .last("LIMIT 1"));
    }

    private LambdaQueryWrapper<RagIngestionTaskEvent> eventQuery(Long taskId, Long afterEventId) {
        return new LambdaQueryWrapper<RagIngestionTaskEvent>()
                .eq(RagIngestionTaskEvent::getTaskId, taskId)
                .gt(afterEventId != null, RagIngestionTaskEvent::getId, afterEventId);
    }

    private LambdaQueryWrapper<RagIngestionTaskShard> shardQuery(Long taskId, String stageCode, String shardStatus) {
        return new LambdaQueryWrapper<RagIngestionTaskShard>()
                .eq(RagIngestionTaskShard::getTaskId, taskId)
                .eq(StringUtils.hasText(stageCode), RagIngestionTaskShard::getStageCode, stageCode)
                .eq(StringUtils.hasText(shardStatus), RagIngestionTaskShard::getShardStatus, shardStatus);
    }

    private PageQuery normalizePageQuery(PageQuery pageQuery, String defaultSort) {
        if (pageQuery == null) {
            return PageQuery.of(1, 50, 50, defaultSort, "ASC", 500);
        }
        return PageQuery.of(
                pageQuery.pageNo(),
                pageQuery.pageSize(),
                pageQuery.pageSize(),
                pageQuery.sortBy(),
                pageQuery.sortDirection(),
                500
        ).withDefaultSort(defaultSort, "ASC");
    }

    private long countStatus(List<RagIngestionTaskShard> shards, IngestionShardStatus status) {
        return shards.stream()
                .filter(shard -> status.name().equals(shard.getShardStatus()))
                .count();
    }

    private String truncate(String value, int max) {
        if (!StringUtils.hasText(value) || value.length() <= max) {
            return value;
        }
        return value.substring(0, max);
    }

    private String jsonEscape(String value) {
        return value.replace("\\", "\\\\").replace("\"", "\\\"");
    }
}
