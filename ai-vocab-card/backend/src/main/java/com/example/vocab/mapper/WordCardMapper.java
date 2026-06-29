package com.example.vocab.mapper;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.vocab.entity.WordCard;
import org.apache.ibatis.annotations.*;
import java.util.List;

@Mapper
public interface WordCardMapper extends BaseMapper<WordCard> {
  @Select("""
    SELECT * FROM word_card
    WHERE (status IS NULL OR status = 1) AND (
      word LIKE CONCAT('%', #{keyword}, '%')
      OR english_definition LIKE CONCAT('%', #{keyword}, '%')
      OR chinese_meaning LIKE CONCAT('%', #{keyword}, '%')
      OR tags LIKE CONCAT('%', #{keyword}, '%')
    )
    ORDER BY updated_at DESC
    LIMIT #{offset}, #{size}
  """)
  List<WordCard> search(@Param("keyword") String keyword, @Param("offset") int offset, @Param("size") int size);

  @Select("""
    SELECT COUNT(*) FROM word_card
    WHERE (status IS NULL OR status = 1) AND (
      word LIKE CONCAT('%', #{keyword}, '%')
      OR english_definition LIKE CONCAT('%', #{keyword}, '%')
      OR chinese_meaning LIKE CONCAT('%', #{keyword}, '%')
      OR tags LIKE CONCAT('%', #{keyword}, '%')
    )
  """)
  long countSearch(@Param("keyword") String keyword);
}
