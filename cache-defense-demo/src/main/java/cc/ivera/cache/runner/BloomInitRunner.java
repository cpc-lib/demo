
package cc.ivera.cache.runner;

import cc.ivera.cache.mapper.OrderMapper;
import cc.ivera.cache.support.OrderBloomFilterSupport;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.CommandLineRunner;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class BloomInitRunner implements CommandLineRunner {

    private final OrderBloomFilterSupport orderBloomFilter;
    private final OrderMapper orderMapper;

    @Override
    public void run(String... args) {
        orderBloomFilter.initialize();

        List<Long> ids = orderMapper.selectAllIds();

        for (Long id : ids) {
            orderBloomFilter.add(id);
        }
    }
}
