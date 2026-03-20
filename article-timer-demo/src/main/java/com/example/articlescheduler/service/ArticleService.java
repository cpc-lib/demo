package com.example.articlescheduler.service;

import com.example.articlescheduler.dto.ArticleCreateDTO;
import com.example.articlescheduler.entity.Article;

public interface ArticleService {

    Long createArticle(ArticleCreateDTO dto) throws Exception;

    Article getById(Long id);
}
