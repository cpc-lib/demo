package com.example.articlescheduler.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.articlescheduler.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Param;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Update("UPDATE article SET status = 1 WHERE id = #{id} AND status = 0")
    int publishArticle(@Param("id") Long id);
}
