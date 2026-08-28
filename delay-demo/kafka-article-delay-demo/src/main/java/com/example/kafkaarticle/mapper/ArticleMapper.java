package com.example.kafkaarticle.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.kafkaarticle.entity.Article;
import org.apache.ibatis.annotations.Mapper;
import org.apache.ibatis.annotations.Update;

@Mapper
public interface ArticleMapper extends BaseMapper<Article> {

    @Update("UPDATE article SET status = 1 WHERE id = #{id}")
    void updateStatusToPublished(Long id);
}
