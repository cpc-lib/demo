package com.example.vocab.controller.export;

import com.example.vocab.dto.export.CreateExportTaskResponse;
import com.example.vocab.dto.export.ExportTaskResponse;
import com.example.vocab.service.export.ExportTaskService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/export")
@RequiredArgsConstructor
public class ExportController {
    private final ExportTaskService exportTaskService;

    @PostMapping("/anki")
    public CreateExportTaskResponse createAnkiTask() { return exportTaskService.createAnkiTask(); }

    @GetMapping("/{taskId}")
    public ExportTaskResponse detail(@PathVariable Long taskId) { return exportTaskService.detail(taskId); }
}
