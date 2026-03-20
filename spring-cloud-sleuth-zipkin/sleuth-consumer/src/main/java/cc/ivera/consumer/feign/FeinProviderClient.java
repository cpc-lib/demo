package cc.ivera.consumer.feign;

import cc.ivera.consumer.config.RestTemplateConfig;
import cc.ivera.consumer.domain.Goods;
import cc.ivera.consumer.hystrix.fallback.FeinProviderClientFallBack;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "FEIGN-PROVIDER",configuration = RestTemplateConfig.class,fallback = FeinProviderClientFallBack.class)
public interface FeinProviderClient {


    @GetMapping("/goods/findOne/{id}")
    public Goods findOne(@PathVariable("id") int id);
}
