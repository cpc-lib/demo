package cc.ivera.ragdemo.controller;

import cc.ivera.ragdemo.service.KnowledgeIngestionService;
import cc.ivera.ragdemo.service.RagChatService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.util.Map;

@RestController
@RequestMapping("/api")
@RequiredArgsConstructor
public class RagController {

    private final KnowledgeIngestionService ingestionService;
    private final RagChatService ragChatService;

    @PostMapping("/ingest/text")
    public Map<String, Object> ingestText(@Valid @RequestBody IngestTextRequest req) {
        int chunks = ingestionService.ingestText(req.text());
        return Map.of("ok", true, "chunks", chunks);
    }

    @PostMapping(value = "/ingest/file", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public Map<String, Object> ingestFile(@RequestPart("file") MultipartFile file) {
        int chunks = ingestionService.ingestFile(file);
        return Map.of("ok", true, "chunks", chunks, "fileName", file.getOriginalFilename());
    }

    @GetMapping(value = "/chat/stream", produces = MediaType.TEXT_EVENT_STREAM_VALUE)
    public SseEmitter chatStream(@RequestParam("question") String question) {
        return ragChatService.streamAnswer(question);
    }

    @GetMapping("/chat")
    public String chat(@RequestParam("question") String question) {
        return ragChatService.answer(question);
    }
}
