package com.example.vocab.service.export;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.example.vocab.config.RabbitConfig;
import com.example.vocab.dto.export.CreateExportTaskResponse;
import com.example.vocab.dto.export.ExportTaskResponse;
import com.example.vocab.dto.export.RetryExportTaskResponse;
import com.example.vocab.entity.export.ExportTask;
import com.example.vocab.infrastructure.mq.AnkiExportMessage;
import com.example.vocab.mapper.export.ExportTaskMapper;
import com.example.vocab.security.CurrentUser;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class ExportTaskService {
    private final ExportTaskMapper exportTaskMapper;
    private final RabbitTemplate rabbitTemplate;

    @Transactional
    public CreateExportTaskResponse createAnkiTask() {
        Long userId = CurrentUser.requiredId();
        ExportTask task = new ExportTask();
        task.setUserId(userId);
        task.setExportType("ANKI_TSV");
        task.setStatus("PENDING");
        exportTaskMapper.insert(task);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ANKI_EXPORT_ROUTING_KEY, new AnkiExportMessage(task.getId(), userId));
        return CreateExportTaskResponse.builder().taskId(task.getId()).status(task.getStatus()).build();
    }

    public ExportTaskResponse detail(Long taskId) {
        Long userId = CurrentUser.requiredId();
        ExportTask task = exportTaskMapper.selectById(taskId);
        if (task == null || !userId.equals(task.getUserId())) throw new IllegalArgumentException("export task not found");
        return ExportTaskResponse.builder()
                .taskId(task.getId()).exportType(task.getExportType()).status(task.getStatus())
                .fileName(task.getFileName()).fileUrl(task.getFileUrl()).errorMessage(task.getErrorMessage()).build();
    }

    public List<ExportTask> listForUser(Long userId) {
        return exportTaskMapper.selectList(new LambdaQueryWrapper<ExportTask>()
                .eq(ExportTask::getUserId, userId)
                .orderByDesc(ExportTask::getId)
                .last("LIMIT 50"));
    }

    @Transactional
    public RetryExportTaskResponse retry(Long id) {
        ExportTask task = exportTaskMapper.selectById(id);
        if (task == null) throw new IllegalArgumentException("export task not found");
        task.setStatus("PENDING");
        task.setErrorMessage(null);
        exportTaskMapper.updateById(task);
        rabbitTemplate.convertAndSend(RabbitConfig.EXCHANGE, RabbitConfig.ANKI_EXPORT_ROUTING_KEY, id);
        return new RetryExportTaskResponse(id, task.getStatus());
    }
}
