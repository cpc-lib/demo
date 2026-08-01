package cc.ivera.ragdemo.service.ragops;

import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

@Component
@RequiredArgsConstructor(onConstructor_ = {@Autowired})
public class RedisPatternKeyDeletionService {

    private static final long DEFAULT_SCAN_COUNT = 1000L;
    private static final int DEFAULT_DELETE_BATCH_SIZE = 500;

    private final StringRedisTemplate redisTemplate;

    public int deleteByPattern(String pattern) {
        return deleteByPattern(pattern, DEFAULT_DELETE_BATCH_SIZE);
    }

    public int countByPattern(String pattern) {
        if (!StringUtils.hasText(pattern)) {
            return 0;
        }
        int count = 0;
        ScanOptions options = scanOptions(pattern);
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                cursor.next();
                count++;
            }
        }
        return count;
    }

    int deleteByPattern(String pattern, int deleteBatchSize) {
        if (!StringUtils.hasText(pattern)) {
            return 0;
        }
        int batchSize = Math.max(1, deleteBatchSize);
        List<String> batch = new ArrayList<>(batchSize);
        int deleted = 0;
        ScanOptions options = scanOptions(pattern);
        try (Cursor<String> cursor = redisTemplate.scan(options)) {
            while (cursor.hasNext()) {
                batch.add(cursor.next());
                if (batch.size() >= batchSize) {
                    deleted += deleteBatch(batch);
                }
            }
        }
        return deleted + deleteBatch(batch);
    }

    private ScanOptions scanOptions(String pattern) {
        return ScanOptions.scanOptions()
                .match(pattern)
                .count(DEFAULT_SCAN_COUNT)
                .build();
    }

    private int deleteBatch(Collection<String> keys) {
        if (keys.isEmpty()) {
            return 0;
        }
        Long count = redisTemplate.delete(new ArrayList<>(keys));
        keys.clear();
        return count == null ? 0 : count.intValue();
    }
}
