package cc.ivera.consumer.hystrix.fallback;

import cc.ivera.consumer.domain.Goods;
import cc.ivera.consumer.feign.FeinProviderClient;
import org.springframework.stereotype.Component;

@Component
public class FeinProviderClientFallBack implements FeinProviderClient {
    @Override
    public Goods findOne(int id) {
        Goods goods = new Goods();
        goods.setId(1);
        goods.setTitle("触发降级");
        goods.setCount(999);
        goods.setPrice(0);
        return goods;
    }
}
