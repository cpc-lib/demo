package com.example.article.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

import java.time.LocalDateTime;
import java.util.Date;

@Data
public class ArticleRequest {

    @NotBlank
    private String title;

    private String content;

    @NotNull
    private Date publishTime;
}
