package com.example.clickhouse;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
class ClickHouseIntegrationTest {

    @Autowired
    JdbcTemplate jdbcTemplate;

    @Test
    void shouldQueryClickHouse() {
        Integer one = jdbcTemplate.queryForObject("SELECT 1", Integer.class);
        assertThat(one).isEqualTo(1);

        Long cnt = jdbcTemplate.queryForObject("SELECT count() FROM t_order", Long.class);
        assertThat(cnt).isNotNull();
        assertThat(cnt).isGreaterThanOrEqualTo(1L);
    }
}
