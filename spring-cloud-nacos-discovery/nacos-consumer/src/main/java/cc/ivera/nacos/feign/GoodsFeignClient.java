package cc.ivera.nacos.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import cc.ivera.nacos.config.FeignLogConfig;
import cc.ivera.nacos.domain.Goods;

/**
 *
 * feign声明式接口。发起远程调用的。
 *
 String url = "http://EUREKA-PROVIDER/goods/findOne/"+id;
 Goods goods = restTemplate.getForObject(url, Goods.class);
 *
 * 1. 定义接口
 * 2. 接口上添加注解 @FeignClient,设置value属性为 服务提供者的 应用名称
 * 3. 编写调用接口，接口的声明规则 和 提供方接口保持一致。
 * 4. 注入该接口对象，调用接口方法完成远程调用
 *
 */

@FeignClient(value = "nacos-provider",configuration = FeignLogConfig.class,fallback = GoodsFeignClientFallback.class)
public interface GoodsFeignClient {

    @GetMapping("/goods/findOne/{id}")
    public Goods findGoodsById(@PathVariable("id") int id);


    @PostMapping("/goods/add")
    public Goods addGoods(@RequestBody Goods goods);

}
