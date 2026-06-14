package cc.ivera.controller;

import cc.ivera.entity.OrderInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.WxPayService;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.web.bind.annotation.*;

import javax.annotation.Resource;
import java.util.List;

@CrossOrigin //开放前端的跨域访问
@Api(tags = "商品订单管理")
@RestController
@RequestMapping("/api/order-info")
public class OrderInfoController {

    @Resource
    private OrderInfoService orderInfoService;

    @Resource
    private WxPayService wxPayService;

    @ApiOperation("订单列表")
    @GetMapping("/list")
    public R list() {
        List<OrderInfo> list = orderInfoService.listOrderByCreateTimeDesc();
        return R.ok().data("list", list);
    }

    /**
     * 前端进行轮询去查询订单的状态
     * 微信回调服务器的接口去修改订单的状态
     * 查询本地订单状态 等待支付成功,关闭二维码页面
     *
     * @param orderNo
     * @return
     */
    @ApiOperation("查询本地订单状态")
    @GetMapping("/query-order-status/{orderNo}")
    public R queryOrderStatus(@PathVariable String orderNo) throws Exception {
        String orderStatus = orderInfoService.getOrderStatus(orderNo);
        if (OrderStatus.SUCCESS.getType().equals(orderStatus)) {
            return R.ok().setMessage("支付成功"); //支付成功
        } else if (OrderStatus.NOTPAY.getType().equals(orderStatus)) {
            //wxPayService.checkOrderStatus(orderNo);
            return R.ok().setCode(101).setMessage("支付中......");
        } else {
            return R.ok().setCode(101).setMessage("支付中......");
        }
    }

}
