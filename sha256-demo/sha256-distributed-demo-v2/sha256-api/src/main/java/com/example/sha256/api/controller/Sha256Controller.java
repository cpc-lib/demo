package com.example.sha256.api.controller;

import com.example.sha256.api.model.TaskResponse;
import com.example.sha256.api.service.Sha256TaskService;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import reactor.core.publisher.Mono;

import java.util.Map;

@RestController
@RequestMapping("/api/sha256")
public class Sha256Controller {
    private final Sha256TaskService taskService;

    public Sha256Controller(Sha256TaskService taskService) {
        this.taskService = taskService;
    }

    @GetMapping("/tasks/{taskId}")
    public Mono<TaskResponse> getTask(@PathVariable String taskId) {
        return taskService.getTask(taskId);
    }

    @GetMapping("/system")
    public Map<String, String> system() {
        return Map.of(
                "broker", taskService.brokerName(),
                "taskStore", "redis-lua",
                "outbox", "mysql",
                "storage", taskService.storageProvider(),
                "bucket", taskService.storageBucket(),
                "upload", "multipart-resumable",
                "worker", "distributed");
    }
}
