package com.example.versioncache.service.impl;

import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import com.example.versioncache.entity.Article;
import com.example.versioncache.mapper.ArticleMapper;
import com.example.versioncache.service.ArticleService;
import org.springframework.stereotype.Service;

@Service
public class ArticleServiceImpl extends ServiceImpl<ArticleMapper, Article> implements ArticleService {
}
