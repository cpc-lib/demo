package com.example.versioncache.web;

import com.example.versioncache.cache.VersionedArticleCache;
import com.example.versioncache.entity.Article;
import com.example.versioncache.web.dto.ArticleCreateRequest;
import com.example.versioncache.web.dto.ArticleUpdateRequest;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/articles")
public class ArticleController {

    private final VersionedArticleCache versionedArticleCache;

    public ArticleController(VersionedArticleCache versionedArticleCache) {
        this.versionedArticleCache = versionedArticleCache;
    }

    @PostMapping
    public Article create(@RequestBody @Validated ArticleCreateRequest req) {
        Article a = new Article();
        a.setTitle(req.getTitle());
        a.setContent(req.getContent());
        return versionedArticleCache.createAndCache(a);
    }

    @GetMapping("/{id}")
    public Article get(@PathVariable Long id) {
        return versionedArticleCache.getById(id);
    }

    /**
     * 注意：更新时必须带上 dataVersion，否则乐观锁不会生效。
     */
    @PutMapping("/{id}")
    public Article update(@PathVariable Long id,
                          @RequestBody @Validated ArticleUpdateRequest req) {
        Article a = new Article();
        a.setId(id);
        a.setTitle(req.getTitle());
        a.setContent(req.getContent());
        a.setDataVersion(req.getDataVersion());
        return versionedArticleCache.updateAndRefreshCache(a);
    }


    @DeleteMapping("/{id}")
    public Map<String, Object> delete(@PathVariable Long id) {
        Map<String, Object> result = new HashMap<>();
        result.put("result", versionedArticleCache.delete(id));
        return result;
    }

}
