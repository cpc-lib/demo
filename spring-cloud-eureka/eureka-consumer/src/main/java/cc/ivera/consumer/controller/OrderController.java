package cc.ivera.consumer.controller;


import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.client.RestTemplate;
import cc.ivera.consumer.domain.Goods;
import cc.ivera.consumer.feign.GoodsFeignClient;

import java.util.List;

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GoodsFeignClient goodsFeignClient;

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("/goods2/{id}")
    public Goods findGoodsById2(@PathVariable("id") int id) {
        Goods goods = goodsFeignClient.findGoodsById(id);
        return goods;
    }


    @GetMapping("/goods/{id}")
    public Goods findGoodsById(@PathVariable("id") int id) {
        //配置了ha情况下，直接调用出错。
        List<ServiceInstance> instances = discoveryClient.getInstances("EUREKA-PROVIDER");
        ServiceInstance serviceInstance = instances.get(0);
        String host = serviceInstance.getHost();
        int port = serviceInstance.getPort();
        String url = "http://" + host + ":" + port + "/goods/findOne/" + id;
        Goods goods = restTemplate.getForObject(url, Goods.class);
        return goods;
    }


    @GetMapping("/goods1/{id}")
    public Goods findGoodsById1(@PathVariable("id") int id) {
        String url = "http://127.0.0.1:8000/goods/findOne/" + id;
        Goods goods = restTemplate.getForObject(url, Goods.class);
        return goods;
    }

}
