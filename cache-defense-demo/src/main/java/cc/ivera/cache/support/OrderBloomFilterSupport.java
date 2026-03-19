package cc.ivera.cache.support;

import lombok.RequiredArgsConstructor;
import org.redisson.api.RBloomFilter;
import org.redisson.api.RedissonClient;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class OrderBloomFilterSupport {

    private static final String BLOOM_KEY = "order:bloom:redisson";
    private static final long EXPECTED_INSERTIONS = 1_000_000L;
    private static final double FALSE_POSITIVE_RATE = 0.01D;

    private final RedissonClient redissonClient;

    public void initialize() {
        bloomFilter().tryInit(EXPECTED_INSERTIONS, FALSE_POSITIVE_RATE);
    }

    public boolean mightContain(Long id) {
        return bloomFilter().contains(id.toString());
    }

    public void add(Long id) {
        bloomFilter().add(id.toString());
    }

    private RBloomFilter<String> bloomFilter() {
        return redissonClient.getBloomFilter(BLOOM_KEY);
    }
}
