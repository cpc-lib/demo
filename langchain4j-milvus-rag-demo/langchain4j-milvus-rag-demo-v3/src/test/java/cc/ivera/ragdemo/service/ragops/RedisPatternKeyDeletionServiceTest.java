package cc.ivera.ragdemo.service.ragops;

import org.junit.jupiter.api.Test;
import org.springframework.data.redis.core.Cursor;
import org.springframework.data.redis.core.ScanOptions;
import org.springframework.data.redis.core.StringRedisTemplate;

import java.util.Collection;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class RedisPatternKeyDeletionServiceTest {

    @Test
    void deletesMatchedKeysByScanBatchesWithoutUsingKeysCommand() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        Cursor<String> cursor = mock(Cursor.class);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, true, false);
        when(cursor.next()).thenReturn("rag:1", "rag:2", "rag:3");
        when(redisTemplate.delete(argThat((Collection<String> keys) -> containsExactly(keys, "rag:1", "rag:2")))).thenReturn(2L);
        when(redisTemplate.delete(argThat((Collection<String> keys) -> containsExactly(keys, "rag:3")))).thenReturn(1L);

        RedisPatternKeyDeletionService service = new RedisPatternKeyDeletionService(redisTemplate);

        int deleted = service.deleteByPattern("rag:*", 2);

        assertThat(deleted).isEqualTo(3);
        verify(redisTemplate, never()).keys(anyString());
        verify(redisTemplate, times(2)).delete(any(Collection.class));
        verify(cursor).close();
    }

    @Test
    void returnsZeroWhenPatternIsBlank() {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        RedisPatternKeyDeletionService service = new RedisPatternKeyDeletionService(redisTemplate);

        int deleted = service.deleteByPattern("   ");

        assertThat(deleted).isZero();
        verify(redisTemplate, never()).scan(any(ScanOptions.class));
        verify(redisTemplate, never()).keys(anyString());
    }

    @Test
    void countsMatchedKeysByScanWithoutUsingKeysCommand() throws Exception {
        StringRedisTemplate redisTemplate = mock(StringRedisTemplate.class);
        @SuppressWarnings("unchecked")
        Cursor<String> cursor = mock(Cursor.class);
        when(redisTemplate.scan(any(ScanOptions.class))).thenReturn(cursor);
        when(cursor.hasNext()).thenReturn(true, true, false);
        when(cursor.next()).thenReturn("rag:1", "rag:2");

        RedisPatternKeyDeletionService service = new RedisPatternKeyDeletionService(redisTemplate);

        int count = service.countByPattern("rag:*");

        assertThat(count).isEqualTo(2);
        verify(redisTemplate, never()).keys(anyString());
        verify(cursor).close();
    }

    private boolean containsExactly(Collection<String> keys, String... expected) {
        return keys != null && List.copyOf(keys).equals(List.of(expected));
    }
}
