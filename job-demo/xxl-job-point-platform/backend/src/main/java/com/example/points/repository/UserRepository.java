package com.example.points.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public class UserRepository {
    private final JdbcTemplate jdbc;

    public UserRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public List<Long> findShardUsers(long lastId, int shardIndex, int shardTotal, int size) {
        return jdbc.queryForList("SELECT id FROM user_account WHERE status='ACTIVE' AND id>? AND MOD(id,?)=? ORDER BY id LIMIT ?", Long.class, lastId, shardTotal, shardIndex, size);
    }

    public long countActive() {
        return jdbc.queryForObject("SELECT COUNT(*) FROM user_account WHERE status='ACTIVE'", Long.class);
    }
}
