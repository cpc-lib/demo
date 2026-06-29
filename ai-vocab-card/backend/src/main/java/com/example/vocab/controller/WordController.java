package com.example.vocab.controller;

import com.example.vocab.dto.*;
import com.example.vocab.service.*;
import com.example.vocab.service.search.SearchFacade;
import com.example.vocab.service.vector.VectorSearchFacade;
import com.example.vocab.quality.Idempotent;
import com.example.vocab.quality.RateLimited;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.*;

@RestController
@RequestMapping("/api/words")
@RequiredArgsConstructor
public class WordController {
  private final AiWordGenerateService aiWordGenerateService;
  private final WordService wordService;
  private final SearchFacade searchFacade;
  private final VectorSearchFacade vectorSearchFacade;

  @RateLimited(key = "word-generate", permitsPerMinute = 20)
  @PostMapping("/generate")
  public WordCardDTO generate(@RequestBody @Valid WordGenerateRequest request) { return aiWordGenerateService.generate(request.getWord()); }

  @Idempotent
  @RateLimited(key = "word-save", permitsPerMinute = 60)
  @PostMapping
  public Map<String, Object> save(@RequestBody @Valid WordCardDTO dto) {
    Long id = wordService.save(dto);
    WordCardDTO saved = wordService.detail(id);
    searchFacade.index(saved);
    vectorSearchFacade.upsert(saved);
    return Map.of("id", id);
  }

  @GetMapping("/search")
  public List<WordCardDTO> search(@RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) { return wordService.search(keyword, page, size); }

  @GetMapping("/search/page")
  public WordSearchPageDTO searchPage(@RequestParam(defaultValue = "") String keyword, @RequestParam(defaultValue = "1") int page, @RequestParam(defaultValue = "10") int size) { return wordService.searchPage(keyword, page, size); }

  @GetMapping("/{id}")
  public WordCardDTO detail(@PathVariable Long id) { return wordService.detail(id); }
}
