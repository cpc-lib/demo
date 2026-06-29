package com.example.vocab.controller.review;

import com.example.vocab.dto.WordCardDTO;
import com.example.vocab.dto.review.*;
import com.example.vocab.service.review.AnkiExportService;
import com.example.vocab.service.review.WordBookService;
import com.example.vocab.security.CurrentUser;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.*;
import org.springframework.web.bind.annotation.*;

import java.nio.charset.StandardCharsets;
import java.util.List;

@RestController
@RequestMapping("/api/wordbook")
@RequiredArgsConstructor
public class WordBookController {
    private final WordBookService wordBookService;
    private final AnkiExportService ankiExportService;

    @PostMapping("/add")
    public ReviewScheduleResponse add(@RequestBody @Valid AddToWordBookRequest request) {
        Long uid = request.getUserId() == null ? CurrentUser.requiredId() : request.getUserId();
        return wordBookService.add(uid, request.getWordCardId());
    }

    @GetMapping("/due")
    public List<WordCardDTO> due(@RequestParam(required = false) Long userId, @RequestParam(defaultValue = "20") int limit) {
        return wordBookService.due(userId == null ? CurrentUser.requiredId() : userId, limit);
    }

    @PostMapping("/review")
    public ReviewScheduleResponse review(@RequestBody @Valid SubmitReviewRequest request) {
        Long uid = request.getUserId() == null ? CurrentUser.requiredId() : request.getUserId();
        return wordBookService.submit(uid, request.getWordCardId(), request.getResult());
    }

    @GetMapping("/export/anki")
    public ResponseEntity<byte[]> exportAnki(@RequestParam(required = false) Long userId) {
        AnkiExportResponse export = ankiExportService.export(userId == null ? CurrentUser.requiredId() : userId);
        ContentDisposition disposition = ContentDisposition.attachment()
                .filename(export.getFileName(), StandardCharsets.UTF_8)
                .build();
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(export.getContentType()))
                .header(HttpHeaders.CONTENT_DISPOSITION, disposition.toString())
                .body(export.getTsvContent().getBytes(StandardCharsets.UTF_8));
    }
}
