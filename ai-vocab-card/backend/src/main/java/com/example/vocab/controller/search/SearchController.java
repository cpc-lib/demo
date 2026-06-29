package com.example.vocab.controller.search;

import com.example.vocab.dto.search.SearchResultDTO;
import com.example.vocab.dto.search.SemanticSearchRequest;
import com.example.vocab.service.search.SearchFacade;
import com.example.vocab.service.vector.VectorSearchFacade;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;
import java.util.List;

@RestController
@RequestMapping("/api/search")
@RequiredArgsConstructor
public class SearchController {
    private final SearchFacade searchFacade;
    private final VectorSearchFacade vectorSearchFacade;

    @GetMapping("/keyword")
    public List<SearchResultDTO> keyword(@RequestParam(defaultValue = "") String keyword,
                                         @RequestParam(defaultValue = "1") int page,
                                         @RequestParam(defaultValue = "10") int size) {
        return searchFacade.keywordSearch(keyword, page, size);
    }

    @PostMapping("/semantic")
    public List<SearchResultDTO> semantic(@RequestBody @Valid SemanticSearchRequest request) {
        return vectorSearchFacade.search(request.getQuery(), request.getTopK() == null ? 10 : request.getTopK());
    }
}
