package cc.ivera.ordermachine.controller;

import cc.ivera.ordermachine.common.ApiResponse;
import cc.ivera.ordermachine.domain.dto.CreateOrderRequest;
import cc.ivera.ordermachine.domain.dto.TransitRequest;
import cc.ivera.ordermachine.domain.entity.Order;
import cc.ivera.ordermachine.service.OrderService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@Validated
@RestController
@RequestMapping("/api/orders")
@RequiredArgsConstructor
public class OrderController {

    private final OrderService orderService;


    @PostMapping
    public ApiResponse<Order> create(@Valid @RequestBody CreateOrderRequest request) {
        return ApiResponse.success("创建成功", orderService.create(request));
    }

    @GetMapping("/{id}")
    public ApiResponse<Order> detail(@PathVariable Long id) {
        return ApiResponse.success(orderService.getById(id));
    }

    @GetMapping
    public ApiResponse<List<Order>> list() {
        return ApiResponse.success(orderService.list());
    }

    @PostMapping("/transit")
    public ApiResponse<Order> transit(@Valid @RequestBody TransitRequest request) {
        Order order = orderService.transit(request.getOrderId(), request.getEvent(), request.getOperator(), request.getRemark());
        return ApiResponse.success("流转成功", order);
    }

    @PostMapping("/{id}/pay")
    public ApiResponse<Order> pay(@PathVariable Long id, @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success("支付成功", orderService.transit(id, "PAY", operator, "订单支付"));
    }

    @PostMapping("/{id}/ship")
    public ApiResponse<Order> ship(@PathVariable Long id, @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success("发货成功", orderService.transit(id, "SHIP", operator, "订单发货"));
    }

    @PostMapping("/{id}/confirm-receipt")
    public ApiResponse<Order> confirmReceipt(@PathVariable Long id, @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success("确认收货成功", orderService.transit(id, "CONFIRM_RECEIPT", operator, "确认收货"));
    }

    @PostMapping("/{id}/cancel")
    public ApiResponse<Order> cancel(@PathVariable Long id, @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success("取消成功", orderService.transit(id, "CANCEL", operator, "取消订单"));
    }

    @PostMapping("/{id}/apply-refund")
    public ApiResponse<Order> applyRefund(@PathVariable Long id, @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success("申请退款成功", orderService.transit(id, "APPLY_REFUND", operator, "申请退款"));
    }

    @PostMapping("/{id}/refund-success")
    public ApiResponse<Order> refundSuccess(@PathVariable Long id, @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success("退款成功", orderService.transit(id, "REFUND_SUCCESS", operator, "退款成功"));
    }

    @PostMapping("/{id}/refund-reject")
    public ApiResponse<Order> refundReject(@PathVariable Long id, @RequestParam(defaultValue = "system") String operator) {
        return ApiResponse.success("退款驳回成功", orderService.transit(id, "REFUND_REJECT", operator, "退款驳回"));
    }
}
