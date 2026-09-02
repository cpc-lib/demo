package com.example.sha256.api.controller;

import com.example.sha256.api.model.CreateTaskResponse;
import com.example.sha256.api.model.TaskResponse;
import com.example.sha256.api.service.Sha256TaskService;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.http.codec.multipart.FilePart;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestPart;
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

    @PostMapping(value = "/tasks", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<CreateTaskResponse>> createTask(@RequestPart("file") FilePart file) {
        return taskService.createTask(file)
                .map(result -> ResponseEntity.status(HttpStatus.ACCEPTED).body(result));
    }

    @GetMapping("/tasks/{taskId}")
    public Mono<TaskResponse> getTask(@PathVariable String taskId) {
        return taskService.getTask(taskId);
    }

    @GetMapping("/system")
    public Map<String, String> system() {
        return Map.of("broker", taskService.brokerName(), "taskStore", "redis", "worker", "distributed");
    }
}
