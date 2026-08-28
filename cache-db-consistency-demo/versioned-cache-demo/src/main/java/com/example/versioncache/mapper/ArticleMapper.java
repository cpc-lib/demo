package com.example.versioncache.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.versioncache.entity.Article;
import org.apache.ibatis.annotations.Mapper;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {
}
