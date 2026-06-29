package com.example.vocab.mapper;

import org.apache.ibatis.annotations.Select;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertTrue;

class WordCardMapperTest {
    @Test
    void searchShouldIncludeLegacyRowsWithoutStatus() throws NoSuchMethodException {
        Select select = WordCardMapper.class
                .getMethod("search", String.class, int.class, int.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value());

        assertTrue(sql.contains("status IS NULL"));
        assertTrue(sql.contains("status = 1"));
    }

    @Test
    void countSearchShouldUseSameSearchPredicate() throws NoSuchMethodException {
        Select select = WordCardMapper.class
                .getMethod("countSearch", String.class)
                .getAnnotation(Select.class);
        String sql = String.join(" ", select.value());

        assertTrue(sql.contains("COUNT(*)"));
        assertTrue(sql.contains("status IS NULL"));
        assertTrue(sql.contains("english_definition LIKE"));
        assertTrue(sql.contains("chinese_meaning LIKE"));
    }
}
