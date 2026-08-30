package cc.ivera.characterization;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.controller.OrderInfoController;
import cc.ivera.controller.PaymentConfigController;
import cc.ivera.controller.ProductController;
import cc.ivera.controller.RefundApplicationController;
import cc.ivera.dto.RefundRequest;
import cc.ivera.entity.Product;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.security.AuthContext;
import cc.ivera.security.AuthUser;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.ProductService;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.vo.R;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class PublicApiCharacterizationTest {

    @AfterEach
    void clearAuthContext() {
        AuthContext.clear();
    }

    @Test
    @DisplayName("现状: ProductController.test 返回 R 包装, message=hello, data 中包含 now")
    void current_product_test_endpoint_returns_wrapped_hello_and_now() {
        ProductController controller = new ProductController(mock(ProductService.class));

        R<Map<String, Object>> response = controller.test();

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("成功");
        assertThat(response.getData()).containsEntry("message", "hello");
        assertThat(response.getData().get("now")).isInstanceOf(Date.class);
    }

    @Test
    @DisplayName("现状: ProductController.list 使用 productList 作为商品列表 data key")
    void current_product_list_endpoint_uses_product_list_data_key() {
        ProductService productService = mock(ProductService.class);
        Product product = new Product();
        product.setId(1L);
        product.setTitle("Java课程");
        product.setPrice(1);
        List<Product> products = Arrays.asList(product);
        when(productService.list()).thenReturn(products);
        ProductController controller = new ProductController(productService);

        R<Map<String, Object>> response = controller.list();

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("成功");
        assertThat(response.getData()).containsEntry("productList", products);
    }

    @Test
    @DisplayName("现状: 订单轮询查到支付成功时 code=0,message=支付成功")
    void current_order_status_success_returns_success_message() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        AuthUser authUser = authenticateUser();
        when(orderInfoService.getOrderStatusForUser("ORD-1", authUser)).thenReturn(OrderStatus.SUCCESS.getType());
        OrderInfoController controller = new OrderInfoController(orderInfoService);

        R<Map<String, Object>> response = controller.queryOrderStatus("ORD-1");

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("支付成功");
    }

    @Test
    @DisplayName("现状: 订单轮询查到未支付时 code=101,message=支付中......")
    void current_order_status_notpay_returns_polling_code_101() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        AuthUser authUser = authenticateUser();
        when(orderInfoService.getOrderStatusForUser("ORD-2", authUser)).thenReturn(OrderStatus.NOTPAY.getType());
        OrderInfoController controller = new OrderInfoController(orderInfoService);

        R<Map<String, Object>> response = controller.queryOrderStatus("ORD-2");

        assertThat(response.getCode()).isEqualTo(101);
        assertThat(response.getMessage()).isEqualTo("支付中......");
    }

    @Test
    @DisplayName("现状: 订单轮询查到空状态仍返回支付中 code=101")
    void current_order_status_null_still_returns_polling_code_101() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        AuthUser authUser = authenticateUser();
        when(orderInfoService.getOrderStatusForUser("ORD-MISSING", authUser)).thenReturn(null);
        OrderInfoController controller = new OrderInfoController(orderInfoService);

        R<Map<String, Object>> response = controller.queryOrderStatus("ORD-MISSING");

        assertThat(response.getCode()).isEqualTo(101);
        assertThat(response.getMessage()).isEqualTo("支付中......");
    }

    @Test
    @DisplayName("现状: 订单轮询查到非成功非未支付状态仍返回支付中 code=101")
    void current_order_status_closed_still_returns_polling_code_101() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        AuthUser authUser = authenticateUser();
        when(orderInfoService.getOrderStatusForUser("ORD-CLOSED", authUser)).thenReturn(OrderStatus.CLOSED.getType());
        OrderInfoController controller = new OrderInfoController(orderInfoService);

        R<Map<String, Object>> response = controller.queryOrderStatus("ORD-CLOSED");

        assertThat(response.getCode()).isEqualTo(101);
        assertThat(response.getMessage()).isEqualTo("支付中......");
    }

    @Test
    @DisplayName("现状: 新退款申请入口透传 orderNo/refundAmount/reason 并返回待审核消息")
    void current_refund_apply_body_endpoint_delegates_and_returns_pending_review_message() {
        RefundApplicationService refundApplicationService = mock(RefundApplicationService.class);
        RefundApplicationController controller = new RefundApplicationController(refundApplicationService);
        RefundRequest request = new RefundRequest();
        request.setOrderNo("ORD-REFUND");
        request.setRefundAmount(50);
        request.setReason("reason");

        R<Map<String, Object>> response = controller.apply(request);

        verify(refundApplicationService).createApplication("ORD-REFUND", 50, "reason");
        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("退款申请单创建成功，待审核");
    }

    @Test
    @DisplayName("现状: 旧退款申请入口将 refundAmount 作为 null 传给服务")
    void current_legacy_refund_apply_endpoint_passes_null_refund_amount() {
        RefundApplicationService refundApplicationService = mock(RefundApplicationService.class);
        RefundApplicationController controller = new RefundApplicationController(refundApplicationService);

        R<Map<String, Object>> response = controller.applyLegacy("ORD-LEGACY", "legacy reason");

        verify(refundApplicationService).createApplication("ORD-LEGACY", null, "legacy reason");
        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("退款申请单创建成功，待审核");
    }

    @Test
    @DisplayName("现状: payment-config/apps 直接返回 PaymentAppConfig 缓存对象")
    void current_payment_config_apps_endpoint_returns_runtime_config_objects() {
        PaymentConfigLoader paymentConfigLoader = mock(PaymentConfigLoader.class);
        PaymentAppConfig appConfig = new PaymentAppConfig();
        appConfig.setAppId(7L);
        appConfig.setAppName("默认微信应用");
        appConfig.setAppid("wx-app-id-current");
        appConfig.setMchId("merchant-current");
        Map<Long, PaymentAppConfig> configs = new HashMap<>();
        configs.put(7L, appConfig);
        when(paymentConfigLoader.getAllAppConfigs()).thenReturn(configs);
        PaymentConfigController controller = new PaymentConfigController(paymentConfigLoader);

        R<Map<Long, PaymentAppConfig>> response = controller.apps();

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("成功");
        assertThat(response.getData()).isSameAs(configs);
        assertThat(response.getData().get(7L).getAppid()).isEqualTo("wx-app-id-current");
        assertThat(response.getData().get(7L).getMchId()).isEqualTo("merchant-current");
    }

    private AuthUser authenticateUser() {
        AuthUser authUser = new AuthUser(1L, "characterization-user", UserRole.USER);
        AuthContext.setUser(authUser);
        return authUser;
    }
}
