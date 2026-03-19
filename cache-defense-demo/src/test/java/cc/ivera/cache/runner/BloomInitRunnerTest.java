package cc.ivera.cache.runner;

import cc.ivera.cache.mapper.OrderMapper;
import cc.ivera.cache.support.OrderBloomFilterSupport;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@SuppressWarnings("unchecked")
class BloomInitRunnerTest {

    @Mock
    private OrderBloomFilterSupport orderBloomFilter;

    @Mock
    private OrderMapper orderMapper;

    @Test
    void initializesFilterEvenWhenNoIdsExist() throws Exception {
        when(orderMapper.selectAllIds()).thenReturn(List.of());

        BloomInitRunner runner = new BloomInitRunner(orderBloomFilter, orderMapper);
        runner.run();

        verify(orderBloomFilter).initialize();
    }

    @Test
    void initializesFilterAndAddsEveryId() throws Exception {
        when(orderMapper.selectAllIds()).thenReturn(List.of(1L, 2L));

        BloomInitRunner runner = new BloomInitRunner(orderBloomFilter, orderMapper);
        runner.run();

        verify(orderBloomFilter).initialize();
        verify(orderBloomFilter, times(2)).add(org.mockito.ArgumentMatchers.anyLong());
    }
}
