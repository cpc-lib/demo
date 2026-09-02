package com.example.sha256.controller;

import com.example.sha256.model.CreateTaskResponse;
import com.example.sha256.model.Sha256Task;
import com.example.sha256.model.TaskResponse;
import com.example.sha256.service.Sha256TaskService;
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

import java.nio.file.Path;

@RestController
@RequestMapping("/api/sha256/tasks")
public class Sha256Controller {

    private final Sha256TaskService taskService;

    public Sha256Controller(Sha256TaskService taskService) {
        this.taskService = taskService;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Mono<ResponseEntity<CreateTaskResponse>> upload(@RequestPart("file") FilePart file) {
        Sha256Task task = taskService.createTask(file.filename());
        Path uploadPath = taskService.allocateUploadPath(task.getTaskId());

        return file.transferTo(uploadPath)
                .then(Mono.fromRunnable(() -> taskService.submit(task.getTaskId(), uploadPath)))
                .thenReturn(ResponseEntity.accepted().body(new CreateTaskResponse(
                        task.getTaskId(),
                        task.getFileName(),
                        task.getStatus(),
                        "/api/sha256/tasks/" + task.getTaskId()
                )))
                .onErrorResume(throwable -> {
                    taskService.failUpload(task.getTaskId(), uploadPath, throwable);
                    return Mono.just(ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                            .body(new CreateTaskResponse(
                                    task.getTaskId(),
                                    task.getFileName(),
                                    task.getStatus(),
                                    "/api/sha256/tasks/" + task.getTaskId()
                            )));
                });
    }

    @GetMapping("/{taskId}")
    public Mono<ResponseEntity<TaskResponse>> getTask(@PathVariable String taskId) {
        return Mono.just(taskService.find(taskId)
                .map(ResponseEntity::ok)
                .orElseGet(() -> ResponseEntity.notFound().build()));
    }
}
