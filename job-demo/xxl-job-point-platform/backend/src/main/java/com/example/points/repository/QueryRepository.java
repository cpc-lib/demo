package com.example.points.repository;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Repository
public class QueryRepository {
    private final JdbcTemplate jdbc;

    public QueryRepository(JdbcTemplate jdbc) {
        this.jdbc = jdbc;
    }

    public Map<String, Object> dashboard(LocalDate d) {
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("totalUsers", val("SELECT COUNT(*) FROM user_account WHERE status='ACTIVE'"));
        m.put("totalPoints", val("SELECT COALESCE(SUM(points),0) FROM user_account"));
        m.put("rewardedUsers", val("SELECT COUNT(*) FROM user_point_ledger WHERE reward_date=? AND biz_type='DAILY_REWARD'", d));
        m.put("rewardedPoints", val("SELECT COALESCE(SUM(change_point),0) FROM user_point_ledger WHERE reward_date=? AND biz_type='DAILY_REWARD'", d));
        m.put("openFailures", val("SELECT COUNT(*) FROM point_reward_failure WHERE status IN ('OPEN','RETRYING')"));
        return m;
    }

    public List<Map<String, Object>> batches(int limit) {
        return jdbc.queryForList("SELECT id,biz_type,reward_date,status,shard_total,scanned_count,sent_count,started_at,finished_at,remark FROM point_reward_batch ORDER BY id DESC LIMIT ?", limit);
    }

    public List<Map<String, Object>> ledgers(LocalDate d, Long userId, int limit) {
        StringBuilder s = new StringBuilder("SELECT id,user_id,batch_id,biz_type,biz_no,change_point,reward_date,created_at FROM user_point_ledger WHERE 1=1");
        List<Object> a = new ArrayList<>();
        if (d != null) {
            s.append(" AND reward_date=?");
            a.add(d);
        }
        if (userId != null) {
            s.append(" AND user_id=?");
            a.add(userId);
        }
        s.append(" ORDER BY id DESC LIMIT ?");
        a.add(limit);
        return jdbc.queryForList(s.toString(), a.toArray());
    }

    public List<Map<String, Object>> failures(int limit) {
        return jdbc.queryForList("SELECT id,batch_id,user_id,biz_no,biz_type,points,reward_date,status,attempt_count,last_error,updated_at FROM point_reward_failure ORDER BY id DESC LIMIT ?", limit);
    }

    public Map<String, Object> reconciliation(LocalDate d, long points) {
        long active = val("SELECT COUNT(*) FROM user_account WHERE status='ACTIVE'");
        long rewarded = val("SELECT COUNT(*) FROM user_point_ledger WHERE reward_date=? AND biz_type='DAILY_REWARD'", d);
        long actual = val("SELECT COALESCE(SUM(change_point),0) FROM user_point_ledger WHERE reward_date=? AND biz_type='DAILY_REWARD'", d);
        Map<String, Object> m = new LinkedHashMap<>();
        m.put("date", d);
        m.put("activeUsers", active);
        m.put("rewardedUsers", rewarded);
        m.put("missingUsers", Math.max(0, active - rewarded));
        m.put("expectedPoints", active * points);
        m.put("actualPoints", actual);
        m.put("difference", active * points - actual);
        m.put("balanced", active == rewarded && active * points == actual);
        return m;
    }

    public List<Map<String, Object>> missing(LocalDate d, int limit) {
        return jdbc.queryForList("SELECT u.id,u.username FROM user_account u LEFT JOIN user_point_ledger l ON l.user_id=u.id AND l.reward_date=? AND l.biz_type='DAILY_REWARD' WHERE u.status='ACTIVE' AND l.id IS NULL ORDER BY u.id LIMIT ?", d, limit);
    }

    private long val(String s, Object... args) {
        Long v = jdbc.queryForObject(s, Long.class, args);
        return v == null ? 0 : v;
    }
}
