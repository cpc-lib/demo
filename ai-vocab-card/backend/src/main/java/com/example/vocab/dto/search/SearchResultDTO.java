package com.example.vocab.dto.search;

import com.example.vocab.dto.WordCardDTO;
import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SearchResultDTO {
    private Long id;
    private String word;
    private String chineseMeaning;
    private String englishDefinition;
    private Double score;
    private String source;
    private WordCardDTO detail;
}
