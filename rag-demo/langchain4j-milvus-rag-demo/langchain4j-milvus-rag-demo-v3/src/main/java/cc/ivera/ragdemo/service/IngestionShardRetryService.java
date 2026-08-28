package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.domain.rag.IngestionShardStatus;
import cc.ivera.ragdemo.domain.rag.IngestionTaskStatus;
import cc.ivera.ragdemo.domain.rag.RagIngestionTask;
import cc.ivera.ragdemo.domain.rag.RagIngestionTaskShard;
import cc.ivera.ragdemo.mapper.RagIngestionTaskMapper;
import cc.ivera.ragdemo.mapper.RagIngestionTaskShardMapper;
import cc.ivera.ragdemo.model.knowledge.IngestionShardRetryRequest;
import cc.ivera.ragdemo.model.knowledge.IngestionShardRetryResponse;
import cc.ivera.ragdemo.model.knowledge.RagIngestionTaskMessage;
import cc.ivera.ragdemo.service.ragops.IngestionShardRetryPolicy;
import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.LambdaUpdateWrapper;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class IngestionShardRetryService {

    private static final int DEFAULT_MAX_RETRY_COUNT = 3;

    private final RagIngestionTaskShardMapper shardMapper;
    private final RagIngestionTaskMapper taskMapper;
    private final RagIngestionTaskPublisher taskPublisher;
    private final IngestionTaskProgressService progressService;

    @Transactional
    public RagIngestionTaskShard markShardRunning(Long taskId,
                                                  String stageCode,
                                                  Long documentId,
                                                  Long documentVersionId,
                                                  String shardKey,
                                                  String shardType,
                                                  int shardIndex,
                                                  String inputHash) {
        RagIngestionTaskShard shard = findOrCreate(taskId, shardKey, shardType);
        LocalDateTime now = LocalDateTime.now();
        shard.setStageCode(stageCode);
        shard.setDocumentId(documentId);
        shard.setDocumentVersionId(documentVersionId);
        shard.setShardIndex(shardIndex);
        shard.setInputHash(inputHash);
        shard.setShardStatus(IngestionShardStatus.RUNNING.name());
        shard.setErrorCode(null);
        shard.setErrorMessage(null);
        shard.setUpdatedAt(now);
        update(shard);
        return shard;
    }

    @Transactional
    public void markShardSuccess(Long taskId, String shardKey, String shardType, String outputRef) {
        RagIngestionTaskShard shard = findOrCreate(taskId, shardKey, shardType);
        shard.setShardStatus(IngestionShardStatus.SUCCESS.name());
        shard.setOutputRef(outputRef);
        shard.setNextRetryAt(null);
        shard.setErrorCode(null);
        shard.setErrorMessage(null);
        shard.setUpdatedAt(LocalDateTime.now());
        update(shard);
    }

    @Transactional
    public void markShardFailed(Long taskId, String shardKey, String shardType, Throwable error) {
        RagIngestionTaskShard shard = findOrCreate(taskId, shardKey, shardType);
        int retryCount = IngestionShardRetryPolicy.nextRetryCount(shard.getRetryCount());
        IngestionShardStatus status = IngestionShardRetryPolicy.failureStatus(error, shard.getRetryCount(), shard.getMaxRetryCount());
        shard.setShardStatus(status.name());
        shard.setRetryCount(retryCount);
        shard.setErrorCode(IngestionShardRetryPolicy.errorCode(error));
        shard.setErrorMessage(IngestionShardRetryPolicy.errorMessage(error, 1900));
        shard.setNextRetryAt(status == IngestionShardStatus.FAILED_RETRYABLE
                ? IngestionShardRetryPolicy.nextRetryAt(LocalDateTime.now(), retryCount - 1)
                : null);
        shard.setUpdatedAt(LocalDateTime.now());
        update(shard);
    }

    @Transactional
    public IngestionShardRetryResponse retryFailedShards(Long taskId, IngestionShardRetryRequest request) {
        RagIngestionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Ingestion task not found: " + taskId);
        }
        IngestionTaskStatus status = IngestionTaskStatus.fromCode(task.getTaskStatus());
        if (status == IngestionTaskStatus.RUNNING || status == IngestionTaskStatus.PENDING || status == IngestionTaskStatus.RETRY_WAIT) {
            throw new IllegalStateException("Task is not in a final state and cannot retry shards: " + status);
        }

        LambdaQueryWrapper<RagIngestionTaskShard> query = new LambdaQueryWrapper<RagIngestionTaskShard>()
                .eq(RagIngestionTaskShard::getTaskId, taskId)
                .eq(RagIngestionTaskShard::getShardStatus, IngestionShardStatus.FAILED_RETRYABLE.name())
                .eq(request != null && StringUtils.hasText(request.stageCode()), RagIngestionTaskShard::getStageCode, request == null ? null : request.stageCode());
        if (request != null && !CollectionUtils.isEmpty(request.shardIds())) {
            query.in(RagIngestionTaskShard::getId, request.shardIds());
        }
        List<RagIngestionTaskShard> shards = shardMapper.selectList(query);
        if (shards.isEmpty()) {
            return new IngestionShardRetryResponse(taskId, 0, 0, false);
        }

        List<Long> ids = shards.stream().map(RagIngestionTaskShard::getId).toList();
        shardMapper.update(null, new LambdaUpdateWrapper<RagIngestionTaskShard>()
                .in(RagIngestionTaskShard::getId, ids)
                .set(RagIngestionTaskShard::getShardStatus, IngestionShardStatus.PENDING.name())
                .set(RagIngestionTaskShard::getNextRetryAt, null)
                .set(RagIngestionTaskShard::getErrorCode, null)
                .set(RagIngestionTaskShard::getErrorMessage, null)
                .set(RagIngestionTaskShard::getUpdatedAt, LocalDateTime.now()));

        LocalDateTime now = LocalDateTime.now();
        task.setTaskStatus(IngestionTaskStatus.RETRY_WAIT.code());
        task.setProgress(0);
        task.setStageProgress(0);
        task.setCurrentStage(null);
        task.setCancelRequested(false);
        task.setCancelRequestedAt(null);
        task.setCancelRequestedBy(null);
        task.setPartialSuccess(false);
        task.setNextRetryAt(now);
        task.setStartedAt(null);
        task.setFinishedAt(null);
        task.setErrorCode(null);
        task.setErrorMessage(null);
        task.setUpdatedAt(now);
        taskMapper.updateById(task);

        boolean published = taskPublisher.publishWithCurrentTrace(new RagIngestionTaskMessage(
                task.getTenantId(),
                task.getId(),
                task.getDocumentId(),
                task.getKnowledgeBaseId(),
                task.getDocumentVersionId(),
                task.getTaskNo()
        ));
        progressService.emitTaskTerminalEvent(taskId, "SHARD_RETRY_REQUESTED", "Retryable shards were reset and task was requeued");
        return new IngestionShardRetryResponse(taskId, shards.size(), ids.size(), published);
    }

    private RagIngestionTaskShard findOrCreate(Long taskId, String shardKey, String shardType) {
        RagIngestionTaskShard existing = shardMapper.selectOne(new LambdaQueryWrapper<RagIngestionTaskShard>()
                .eq(RagIngestionTaskShard::getTaskId, taskId)
                .eq(RagIngestionTaskShard::getShardKey, shardKey)
                .eq(RagIngestionTaskShard::getShardType, shardType)
                .last("LIMIT 1"));
        if (existing != null) {
            return existing;
        }
        LocalDateTime now = LocalDateTime.now();
        RagIngestionTaskShard shard = new RagIngestionTaskShard();
        shard.setTaskId(taskId);
        shard.setShardKey(shardKey);
        shard.setShardType(shardType);
        shard.setShardStatus(IngestionShardStatus.PENDING.name());
        shard.setRetryCount(0);
        shard.setMaxRetryCount(DEFAULT_MAX_RETRY_COUNT);
        shard.setCreatedAt(now);
        shard.setUpdatedAt(now);
        shardMapper.insert(shard);
        return shard;
    }

    private void update(RagIngestionTaskShard shard) {
        if (shard.getId() == null) {
            shardMapper.insert(shard);
            return;
        }
        shardMapper.updateById(shard);
    }
}
