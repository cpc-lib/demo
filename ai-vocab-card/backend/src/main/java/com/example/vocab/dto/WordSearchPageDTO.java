package com.example.vocab.dto;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class WordSearchPageDTO {
    private long total;
    private int page;
    private int size;
    private List<WordCardDTO> items;
}
