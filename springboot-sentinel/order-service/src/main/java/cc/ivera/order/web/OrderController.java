package cc.ivera.order.web;

import cc.ivera.order.pojo.Order;
import cc.ivera.order.service.OrderService;
import com.alibaba.csp.sentinel.annotation.SentinelResource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("order")
public class OrderController {

    @Autowired
    private OrderService orderService;

    //热点参数线路仅支持路径参数
    @SentinelResource("hot")
    @GetMapping("{orderId}")
    public Order queryOrderByUserId(@PathVariable("orderId") Long orderId) {
        // 根据id查询订单并返回
        return orderService.queryOrderById(orderId);
    }

    //关联限流
    @GetMapping("/query")
    public String query() {
        return "查询订单成功";
    }

    @GetMapping("/update")
    public String update() {
        return "更新订单成功";
    }

    //链路限流
    @GetMapping("/queryOrder")
    public String queryOrder() {
        //查询商品
        orderService.queryGoods();
        //查询订单
        System.out.println("查询订单");
        return "查询订单成功";
    }

    @GetMapping("/save")
    public String save() {
        //查询商品
        orderService.queryGoods();
        //保存订单
        System.out.println("保存订单");
        return "保存订单成功";
    }
}
