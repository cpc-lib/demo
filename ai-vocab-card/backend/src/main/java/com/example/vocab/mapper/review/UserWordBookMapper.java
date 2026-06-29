package com.example.vocab.mapper.review;

import com.baomidou.mybatisplus.core.mapper.BaseMapper;
import com.example.vocab.entity.review.UserWordBook;
import org.apache.ibatis.annotations.*;
import java.time.LocalDateTime;
import java.util.List;

@Mapper
public interface UserWordBookMapper extends BaseMapper<UserWordBook> {
    @Select("""
        SELECT * FROM user_word_book
        WHERE user_id = #{userId}
          AND next_review_time <= #{now}
        ORDER BY next_review_time ASC
        LIMIT #{limit}
    """)
    List<UserWordBook> findDue(@Param("userId") Long userId, @Param("now") LocalDateTime now, @Param("limit") int limit);
}
