package com.example.vocab.dto.review;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class AnkiExportResponse {
    private String fileName;
    private String contentType;
    private String tsvContent;
}
