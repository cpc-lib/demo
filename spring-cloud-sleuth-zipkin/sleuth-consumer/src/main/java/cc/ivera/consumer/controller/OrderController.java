package cc.ivera.consumer.controller;


import cc.ivera.consumer.domain.Goods;
import cc.ivera.consumer.feign.FeinProviderClient;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/order")
public class OrderController {


    @Autowired
    private FeinProviderClient client;


    @GetMapping("/goods/{id}")
    public Goods findGoodsById(@PathVariable("id") int id){

        //使用Feign 完成远程调用
        Goods goods = client.findOne(id);

        return goods;
    }


}
