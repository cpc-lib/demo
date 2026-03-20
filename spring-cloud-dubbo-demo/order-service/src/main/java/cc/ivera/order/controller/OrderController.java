package cc.ivera.order.controller;

import cc.ivera.dubbo.api.UserService;
import cc.ivera.dubbo.domain.Order;
import cc.ivera.dubbo.domain.User;
import cc.ivera.order.service.OrderService;
import org.apache.dubbo.config.annotation.DubboReference;
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

    @DubboReference
    private UserService userService;


    @GetMapping("{orderId}")
    public Order queryOrderByUserId(@PathVariable("orderId") Long orderId) {
        //1、根据id查询订单
        Order order = orderService.queryOrderById(orderId);
        //2、获取订单中的用户id
        Long userId = order.getUserId();
        //3、根据用户id，调用dubbo服务查询用户
        User user = userService.queryById(userId);
        order.setUser(user);
        //4、返回
        return order;
    }
}
