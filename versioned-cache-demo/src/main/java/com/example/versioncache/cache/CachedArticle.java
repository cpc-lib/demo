package com.example.versioncache.cache;

import com.example.versioncache.entity.Article;

public class CachedArticle {

    private Long version;

    private Article article;

    public Long getVersion() {
        return version;
    }

    public void setVersion(Long version) {
        this.version = version;
    }

    public Article getArticle() {
        return article;
    }

    public void setArticle(Article article) {
        this.article = article;
    }
}
