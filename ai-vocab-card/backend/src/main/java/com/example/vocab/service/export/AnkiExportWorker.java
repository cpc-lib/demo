package com.example.vocab.service.export;

import com.example.vocab.config.RabbitConfig;
import com.example.vocab.dto.review.AnkiExportResponse;
import com.example.vocab.entity.export.ExportTask;
import com.example.vocab.infrastructure.mq.AnkiExportMessage;
import com.example.vocab.infrastructure.storage.ObjectStorageService;
import com.example.vocab.mapper.export.ExportTaskMapper;
import com.example.vocab.service.review.AnkiExportService;
import lombok.RequiredArgsConstructor;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class AnkiExportWorker {
    private final ExportTaskMapper exportTaskMapper;
    private final AnkiExportService ankiExportService;
    private final ObjectStorageService objectStorageService;

    @RabbitListener(queues = RabbitConfig.ANKI_EXPORT_QUEUE)
    public void handle(AnkiExportMessage message) {
        ExportTask task = exportTaskMapper.selectById(message.getTaskId());
        if (task == null) return;
        try {
            task.setStatus("RUNNING");
            exportTaskMapper.updateById(task);
            AnkiExportResponse export = ankiExportService.export(message.getUserId());
            String objectName = "anki/" + message.getUserId() + "/" + export.getFileName();
            String url = objectStorageService.putText(objectName, export.getTsvContent(), export.getContentType());
            task.setStatus("SUCCESS");
            task.setFileName(export.getFileName());
            task.setFileUrl(url);
            task.setErrorMessage(null);
            exportTaskMapper.updateById(task);
        } catch (Exception e) {
            task.setStatus("FAILED");
            task.setErrorMessage(e.getMessage());
            exportTaskMapper.updateById(task);
        }
    }
}
