package com.example.vocab.mapper.search;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.vocab.entity.search.WordEmbedding;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface WordEmbeddingMapper extends BaseMapper<WordEmbedding> {
    @Select("""
      SELECT * FROM word_embedding
      WHERE MATCH(content, keywords) AGAINST(#{query} IN NATURAL LANGUAGE MODE)
      ORDER BY updated_at DESC
      LIMIT #{limit}
    """)
    List<WordEmbedding> semanticCandidates(@Param("query") String query, @Param("limit") int limit);
}
