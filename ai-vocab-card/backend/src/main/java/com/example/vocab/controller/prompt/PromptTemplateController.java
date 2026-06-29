package com.example.vocab.controller.prompt;

import com.example.vocab.dto.prompt.PromptTemplateDTO;
import com.example.vocab.service.prompt.PromptTemplateService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/prompts")
@RequiredArgsConstructor
public class PromptTemplateController {
    private final PromptTemplateService service;

    @GetMapping
    public List<PromptTemplateDTO> list(@RequestParam(required = false) String code) { return service.list(code); }

    @PostMapping
    public Map<String, Object> save(@RequestBody @Valid PromptTemplateDTO dto) { return Map.of("id", service.save(dto)); }

    @DeleteMapping("/{id}")
    public Map<String, Object> disable(@PathVariable Long id) { service.disable(id); return Map.of("success", true); }
}
