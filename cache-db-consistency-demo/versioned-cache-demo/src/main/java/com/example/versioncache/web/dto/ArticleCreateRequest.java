package com.example.versioncache.web.dto;

import javax.validation.constraints.NotBlank;

public class ArticleCreateRequest {

    @NotBlank
    private String title;

    private String content;

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getContent() {
        return content;
    }

    public void setContent(String content) {
        this.content = content;
    }
}
