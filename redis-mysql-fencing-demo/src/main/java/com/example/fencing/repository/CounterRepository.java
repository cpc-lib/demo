package com.example.fencing.repository;

import com.example.fencing.model.CounterSnapshot;
import java.util.Optional;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

@Repository
public class CounterRepository {
    private final JdbcTemplate jdbc;

    public CounterRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Optional<CounterSnapshot> find(String resourceId) {
        return jdbc.query("""
                SELECT resource_id, counter_value, fence_token
                FROM fenced_counter WHERE resource_id = ?
                """, rs -> rs.next()
                ? Optional.of(new CounterSnapshot(
                    rs.getString("resource_id"),
                    rs.getLong("counter_value"),
                    rs.getLong("fence_token")))
                : Optional.empty(), resourceId);
    }
}
