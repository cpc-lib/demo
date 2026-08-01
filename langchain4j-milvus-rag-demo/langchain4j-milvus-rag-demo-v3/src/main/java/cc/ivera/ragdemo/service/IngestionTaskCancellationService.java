package cc.ivera.ragdemo.service;


import cc.ivera.ragdemo.domain.rag.IngestionTaskStatus;
import cc.ivera.ragdemo.domain.rag.RagIngestionTask;
import cc.ivera.ragdemo.mapper.RagIngestionTaskMapper;
import cc.ivera.ragdemo.service.ragops.IngestionCancellationPolicy;
import cc.ivera.ragdemo.service.ragops.IngestionTaskCancelledException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class IngestionTaskCancellationService {

    private final RagIngestionTaskMapper taskMapper;
    private final IngestionTaskProgressService progressService;

    @Transactional
    public RagIngestionTask requestCancel(Long taskId, String operator) {
        RagIngestionTask task = getRequired(taskId);
        IngestionTaskStatus status = IngestionTaskStatus.fromCode(task.getTaskStatus());
        IngestionCancellationPolicy.assertCanCancel(status);

        LocalDateTime now = LocalDateTime.now();
        task.setCancelRequested(true);
        task.setCancelRequestedAt(now);
        task.setCancelRequestedBy(operator);
        task.setNextRetryAt(null);
        task.setErrorCode(status == IngestionTaskStatus.RUNNING ? "TASK_CANCEL_REQUESTED" : "TASK_CANCELLED");
        task.setErrorMessage(status == IngestionTaskStatus.RUNNING
                ? "Cancellation requested; executor will stop at the next checkpoint"
                : "Cancelled by user request");
        task.setUpdatedAt(now);

        IngestionCancellationPolicy.CancelDecision decision = IngestionCancellationPolicy.decide(status);
        if (decision == IngestionCancellationPolicy.CancelDecision.DIRECT_CANCEL) {
            task.setTaskStatus(IngestionTaskStatus.CANCELLED.code());
            task.setFinishedAt(now);
        }
        taskMapper.updateById(task);

        if (decision == IngestionCancellationPolicy.CancelDecision.DIRECT_CANCEL) {
            progressService.cancelRunningStages(taskId);
        } else {
            progressService.emitTaskTerminalEvent(taskId, "TASK_CANCEL_REQUESTED", task.getErrorMessage());
        }
        return task;
    }

    public boolean isCancellationRequested(Long taskId) {
        RagIngestionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            return false;
        }
        return Boolean.TRUE.equals(task.getCancelRequested())
                || IngestionTaskStatus.fromCode(task.getTaskStatus()) == IngestionTaskStatus.CANCELLED;
    }

    public void throwIfCancellationRequested(Long taskId) {
        if (isCancellationRequested(taskId)) {
            throw new IngestionTaskCancelledException(taskId);
        }
    }

    @Transactional
    public void markCancelled(Long taskId) {
        RagIngestionTask task = getRequired(taskId);
        LocalDateTime now = LocalDateTime.now();
        task.setTaskStatus(IngestionTaskStatus.CANCELLED.code());
        task.setCancelRequested(true);
        task.setErrorCode("TASK_CANCELLED");
        task.setErrorMessage("Ingestion task stopped after cancellation request");
        task.setFinishedAt(now);
        task.setHeartbeatAt(now);
        task.setUpdatedAt(now);
        taskMapper.updateById(task);
        progressService.cancelRunningStages(taskId);
    }

    private RagIngestionTask getRequired(Long taskId) {
        RagIngestionTask task = taskMapper.selectById(taskId);
        if (task == null) {
            throw new IllegalArgumentException("Ingestion task not found: " + taskId);
        }
        return task;
    }
}
