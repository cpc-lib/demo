package com.example.sha256.api.controller;

import com.example.sha256.api.upload.MultipartUploadService;
import com.example.sha256.api.upload.UploadApiModels.*;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/api/sha256/uploads")
public class MultipartUploadController {
    private final MultipartUploadService uploadService;

    public MultipartUploadController(MultipartUploadService uploadService) {
        this.uploadService = uploadService;
    }

    @PostMapping("/init")
    public Mono<InitResponse> initialize(@RequestBody InitRequest request) {
        return uploadService.initialize(request);
    }

    @GetMapping("/{sessionId}")
    public Mono<InitResponse> status(@PathVariable String sessionId) {
        return uploadService.status(sessionId);
    }

    @PostMapping("/{sessionId}/presign")
    public Mono<PresignResponse> presign(@PathVariable String sessionId,
                                         @RequestBody PresignRequest request) {
        return uploadService.presign(sessionId, request);
    }

    @PostMapping("/{sessionId}/complete")
    public Mono<ResponseEntity<CompleteResponse>> complete(@PathVariable String sessionId) {
        return uploadService.complete(sessionId)
                .map(result -> ResponseEntity.status(HttpStatus.ACCEPTED).body(result));
    }

    @DeleteMapping("/{sessionId}")
    @ResponseStatus(HttpStatus.NO_CONTENT)
    public Mono<Void> abort(@PathVariable String sessionId) {
        return uploadService.abort(sessionId);
    }
}
