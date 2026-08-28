package com.example.rag.web;

import com.example.rag.service.DocumentService;
import com.example.rag.service.ai.Assistant;
import com.example.rag.web.dto.ChatRequest;
import jakarta.validation.Valid;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api")
public class ChatController {

    private final DocumentService documentService;
    private final Assistant assistant;

    public ChatController(DocumentService documentService, Assistant assistant) {
        this.documentService = documentService;
        this.assistant = assistant;
    }

    @PostMapping(value = "/files/upload", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<String> upload(@RequestPart("file") MultipartFile file) {
        DocumentService.IngestResult r = documentService.ingest(file);
        return ResponseEntity.ok("OK: " + r.filename() + " -> segments=" + r.segments());
    }

    @PostMapping(value = "/chat", consumes = MediaType.APPLICATION_JSON_VALUE, produces = MediaType.TEXT_PLAIN_VALUE)
    public ResponseEntity<String> chat(@Valid @RequestBody ChatRequest req) {
        String answer = assistant.chat(req.getQuestion());
        return ResponseEntity.ok(answer);
    }
}
