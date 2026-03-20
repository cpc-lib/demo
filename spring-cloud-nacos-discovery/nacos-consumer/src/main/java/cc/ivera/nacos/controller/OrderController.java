package cc.ivera.nacos.controller;


import org.springframework.web.bind.annotation.*;
import cc.ivera.nacos.domain.Goods;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.cloud.client.ServiceInstance;
import org.springframework.cloud.client.discovery.DiscoveryClient;
import org.springframework.web.client.RestTemplate;
import cc.ivera.nacos.feign.GoodsFeignClient;

import java.util.List;

/**
 * 服务的调用方
 */

@RestController
@RequestMapping("/order")
public class OrderController {

    @Autowired
    private RestTemplate restTemplate;

    @Autowired
    private GoodsFeignClient goodsFeignClient;

    @Autowired
    private DiscoveryClient discoveryClient;

    @GetMapping("/goods/{id}")
    public Goods findGoodsById(@PathVariable("id") int id){
        //演示discoveryClient 使用
        List<ServiceInstance> instances = discoveryClient.getInstances("nacos-provider");

        //判断集合是否有数据
        if(instances == null || instances.size() == 0){
            //集合没有数据
            return null;
        }

        ServiceInstance instance = instances.get(0);
        String host = instance.getHost();//获取ip
        int port = instance.getPort();//获取端口

        System.out.println(host);
        System.out.println(port);

        String url = "http://"+host+":"+port+"/goods/findOne/"+id;
        // 3. 调用方法
        Goods goods = restTemplate.getForObject(url, Goods.class);
        return goods;
    }



    @GetMapping("/goods2/{id}")
    public Goods findGoodsById2(@PathVariable("id") int id) {
        Goods goods = goodsFeignClient.findGoodsById(id);
        return goods;
    }

    @GetMapping("/goods1/{id}")
    public Goods findGoodsById1(@PathVariable("id") int id) {
        String url = "http://127.0.0.1:8000/goods/findOne/" + id;
        Goods goods = restTemplate.getForObject(url, Goods.class);
        return goods;
    }

    @PostMapping("/goods/add")
    public Goods addGoods(@RequestBody Goods goods) {
        return goodsFeignClient.addGoods(goods);
    }
}
