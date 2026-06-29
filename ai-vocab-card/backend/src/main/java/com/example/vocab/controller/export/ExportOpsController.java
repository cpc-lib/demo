package com.example.vocab.controller.export;

import com.example.vocab.dto.export.RetryExportTaskResponse;
import com.example.vocab.entity.export.ExportTask;
import com.example.vocab.service.export.ExportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/export/tasks")
@RequiredArgsConstructor
public class ExportOpsController {
    private final ExportTaskService exportTaskService;

    @GetMapping
    public List<ExportTask> list(@RequestParam Long userId) {
        return exportTaskService.listForUser(userId);
    }

    @PostMapping("/{id}/retry")
    public RetryExportTaskResponse retry(@PathVariable Long id) {
        return exportTaskService.retry(id);
    }
}
