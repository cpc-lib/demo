package cc.ivera.controller;

import cc.ivera.domain.Good2;
import cc.ivera.service.GoodService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api")
public class GoodController {

    @Autowired
    private GoodService goodService;


    @RequestMapping("/sellGood")
    public String sell() throws InterruptedException {
        Map<String, Object> stringObjectMap = goodService.sellGood3("1586620453682253824");
        return "";
    }

    @GetMapping("/cart")
    public Map<String,Object> goodList(){
        Map<String,Object> retMap=new HashMap<>();
        retMap.put("status",200);
        retMap.put("message","获取购物车列表数据成功!");

        List<Good2> list= new ArrayList<>();

        for (int i = 0; i < 10; i++) {
            Good2 good2 = Good2.builder()
                    .goods_count(1)
                    .goods_img("https://www.escook.cn/vuebase/pics/1.png")
                    .goods_name("班俏BANQIAO超火ins潮卫衣女士2020秋季新款韩版宽松慵懒风薄款外套带帽上衣")
                    .goods_price(108)
                    .good_state(true)
                    .id(i+1).build();
            list.add(good2);
        }
        retMap.put("list",list);
        return retMap;
    }
}
