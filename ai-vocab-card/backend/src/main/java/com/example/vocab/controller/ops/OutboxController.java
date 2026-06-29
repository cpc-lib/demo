package com.example.vocab.controller.ops;

import com.example.vocab.entity.outbox.OutboxEvent;
import com.example.vocab.service.outbox.OutboxService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/ops/outbox")
@RequiredArgsConstructor
public class OutboxController {
    private final OutboxService outboxService;

    @GetMapping
    public List<OutboxEvent> list(@RequestParam(defaultValue = "PENDING") String status) {
        return outboxService.list(status);
    }
}
