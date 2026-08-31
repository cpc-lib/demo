package cc.ivera.controller;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.OrderItem;
import cc.ivera.dto.CheckoutRequest;
import cc.ivera.enums.OrderStatus;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.CheckoutService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.vo.CheckoutResult;
import cc.ivera.vo.R;
import io.swagger.annotations.Api;
import io.swagger.annotations.ApiOperation;
import org.springframework.validation.annotation.Validated;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.Size;
import java.util.List;
import java.util.Map;

@CrossOrigin //开放前端的跨域访问
@Api(tags = "商品订单管理")
@RestController
@RequestMapping("/api/order-info")
@Validated
public class OrderInfoController {

    private final OrderInfoService orderInfoService;

    private final CheckoutService checkoutService;

    @Autowired
    public OrderInfoController(
        OrderInfoService orderInfoService,
        CheckoutService checkoutService
    ) {
        this.orderInfoService = orderInfoService;
        this.checkoutService = checkoutService;
    }

    public OrderInfoController(OrderInfoService orderInfoService) {
        this(orderInfoService, null);
    }

    @ApiOperation("订单列表")
    @GetMapping("/list")
    public R<Map<String, Object>> list() {
        List<OrderInfo> list = orderInfoService.listOrderByCreateTimeDesc();
        return R.ok().data("list", list);
    }

    @ApiOperation("我的订单")
    @GetMapping("/my-list")
    public R<Map<String, Object>> myList() {
        List<OrderInfo> list = orderInfoService.listOrderByUserId(AuthContext.requireUser().getUserId());
        return R.ok().data("list", list);
    }

    @ApiOperation("购物车结算")
    @PostMapping("/checkout")
    public R<CheckoutResult> checkout(@RequestBody @javax.validation.Valid CheckoutRequest request) {
        AuthUser authUser = AuthContext.requireShoppingUser();
        CheckoutResult result = checkoutService.checkout(
                authUser.getUserId(),
                request.getPaymentAppId(),
                request.getCheckoutRequestId()
        );
        return R.ok(result);
    }

    @ApiOperation("订单明细")
    @GetMapping("/{orderNo}/items")
    public R<List<OrderItem>> orderItems(@PathVariable String orderNo) {
        return R.ok(orderInfoService.listOrderItemsForUser(orderNo, AuthContext.requireUser()));
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
    public R<Map<String, Object>> queryOrderStatus(@PathVariable @NotBlank(message = "订单号不能为空") @Size(max = 50, message = "订单号长度不能超过50个字符") String orderNo) {
        String orderStatus = orderInfoService.getOrderStatusForUser(orderNo, AuthContext.requireUser());
        if (OrderStatus.SUCCESS.getType().equals(orderStatus)) {
            return R.ok().setMessage("支付成功"); //支付成功
        } else if (OrderStatus.NOTPAY.getType().equals(orderStatus)) {
            return R.ok().setCode(101).setMessage("支付中......");
        } else {
            return R.ok().setCode(101).setMessage("支付中......");
        }
    }

}
