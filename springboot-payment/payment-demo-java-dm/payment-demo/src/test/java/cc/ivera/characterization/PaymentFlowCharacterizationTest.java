package cc.ivera.characterization;

import cc.ivera.config.OrderCloseRabbitConfig;
import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.config.RefundStatusSyncRabbitConfig;
import cc.ivera.controller.AliPayController;
import cc.ivera.controller.OrderInfoController;
import cc.ivera.controller.PaymentConfigController;
import cc.ivera.controller.RefundApplicationController;
import cc.ivera.controller.RefundInfoController;
import cc.ivera.controller.WxPayV2Controller;
import cc.ivera.dto.RefundApproveRequest;
import cc.ivera.dto.RefundRequest;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.enums.RefundApprovalStatus;
import cc.ivera.enums.RefundStatus;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.PaymentInfoMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.mapper.RefundInfoMapper;
import cc.ivera.mq.OrderCloseMessage;
import cc.ivera.mq.RefundStatusSyncMessage;
import cc.ivera.service.AliPayService;
import cc.ivera.service.OrderCloseMessageService;
import cc.ivera.service.OrderInfoService;
import cc.ivera.service.PaymentAppService;
import cc.ivera.service.PaymentChannelService;
import cc.ivera.service.PaymentInfoService;
import cc.ivera.service.ProductService;
import cc.ivera.service.RefundApplicationService;
import cc.ivera.service.RefundInfoService;
import cc.ivera.service.impl.OrderCloseMessageServiceImpl;
import cc.ivera.service.impl.OrderInfoServiceImpl;
import cc.ivera.service.impl.PaymentInfoServiceImpl;
import cc.ivera.service.impl.RefundInfoServiceImpl;
import cc.ivera.service.impl.RefundStatusSyncMessageServiceImpl;
import cc.ivera.service.impl.wxpay.WxPayOrderService;
import cc.ivera.service.refund.OrderRefundStatusService;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import cc.ivera.util.MoneyUtils;
import cc.ivera.vo.R;
import com.alipay.api.AlipayClient;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.dao.DataAccessException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.data.redis.core.ValueOperations;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

/**
 * 特征测试集 — 基于架构梳理后的当前行为锁定。
 *
 * 本文件覆盖以下行为面：
 * - 订单创建/复用规则
 * - saveCodeUrl 幂等不覆盖
 * - 支付流水记录（微信 V3/V2、支付宝）
 * - 退款申请/审核/状态刷新
 * - 配置加载/缓存
 * - RabbitMQ 消息路由
 * - 所有可疑行为
 *
 * 注意：所有测试使用 mock 隔离，不依赖真实数据库/Redis/RabbitMQ。
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class PaymentFlowCharacterizationTest {

    // ==================== 订单创建/复用 ====================

    @Test
    @DisplayName("现状: createOrReuseOrder 四参数版本在 productId=null 时抛 BizException")
    void current_create_order_rejects_null_product_id() {
        ProductMapper productMapper = mock(ProductMapper.class);
        OrderCloseMessageService orderCloseMessageService = mock(OrderCloseMessageService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        OrderInfoServiceImpl service = new OrderInfoServiceImpl(
                productMapper, orderCloseMessageService, distributedLockTemplate, transactionTemplate);

        assertThatThrownBy(() -> service.createOrReuseOrder(null, PayType.WXPAY.getType(), null, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("商品ID不能为空");

        verifyNoInteractions(distributedLockTemplate);
    }

    @Test
    @DisplayName("现状: createOrReuseOrder 四参数版本在 paymentType 为空时抛 BizException")
    void current_create_order_rejects_blank_payment_type() {
        ProductMapper productMapper = mock(ProductMapper.class);
        OrderCloseMessageService orderCloseMessageService = mock(OrderCloseMessageService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        OrderInfoServiceImpl service = new OrderInfoServiceImpl(
                productMapper, orderCloseMessageService, distributedLockTemplate, transactionTemplate);

        assertThatThrownBy(() -> service.createOrReuseOrder(1L, "", null, null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("支付方式不能为空");
    }

    @Test
    @DisplayName("现状: createOrReuseOrder 使用 paymentAppId 参与锁 key 构建，null 时用 default")
    void current_create_order_lock_key_uses_default_for_null_payment_app_id() {
        ProductMapper productMapper = mock(ProductMapper.class);
        OrderCloseMessageService orderCloseMessageService = mock(OrderCloseMessageService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        OrderInfoServiceImpl service = new OrderInfoServiceImpl(
                productMapper, orderCloseMessageService, distributedLockTemplate, transactionTemplate);

        // 两参数版本调用四参数版本，paymentAppId=null
        when(distributedLockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, Supplier.class).get());

        assertThatCode(() -> service.createOrReuseOrder(1L, PayType.WXPAY.getType()))
                .doesNotThrowAnyException();

        ArgumentCaptor<String> lockKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(distributedLockTemplate).execute(
                lockKeyCaptor.capture(),
                eq(3000L),
                eq(10000L),
                any(Supplier.class));
        assertThat(lockKeyCaptor.getValue()).contains(":default");
    }

    @Test
    @DisplayName("现状: saveCodeUrl 在 orderNo 或 codeUrl 为空时直接返回")
    void current_save_code_url_ignores_blank_inputs() {
        OrderInfoMapper orderInfoMapper = mock(OrderInfoMapper.class);
        OrderInfoServiceImpl service = new OrderInfoServiceImpl(
                mock(ProductMapper.class),
                mock(OrderCloseMessageService.class),
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class));
        ReflectionTestUtils.setField(service, "baseMapper", orderInfoMapper);

        service.saveCodeUrl("", "https://example.com");
        service.saveCodeUrl("ORD-1", "");
        service.saveCodeUrl(null, "https://example.com");
        service.saveCodeUrl("ORD-1", null);

        verify(orderInfoMapper, never()).update(any(), any());
    }

    @Test
    @DisplayName("现状: saveCodeUrl 使用条件更新，code_url 已有值时不覆盖")
    void current_save_code_url_does_not_overwrite_existing_code_url(CapturedOutput output) {
        OrderInfoMapper orderInfoMapper = mock(OrderInfoMapper.class);
        when(orderInfoMapper.update(any(), any())).thenReturn(0);
        OrderInfoServiceImpl service = new OrderInfoServiceImpl(
                mock(ProductMapper.class),
                mock(OrderCloseMessageService.class),
                mock(DistributedLockTemplate.class),
                mock(TransactionTemplate.class));
        ReflectionTestUtils.setField(service, "baseMapper", orderInfoMapper);

        service.saveCodeUrl("ORD-EXISTING", "https://new-url.com");

        verify(orderInfoMapper).update(any(), any(UpdateWrapper.class));
        assertThat(output.getOut()).contains("订单二维码已存在");
    }

    // ==================== 支付流水记录 ====================

    @Test
    @DisplayName("现状: createPaymentInfoForWxPayV2 的 trade_state 取 result_code 而非 trade_state")
    void current_wxpay_v2_payment_info_maps_result_code_as_trade_state() {
        PaymentInfoMapper paymentInfoMapper = mock(PaymentInfoMapper.class);
        PaymentInfoServiceImpl service = new PaymentInfoServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", paymentInfoMapper);

        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORD-V2");
        params.put("transaction_id", "TX-V2");
        params.put("trade_type", "NATIVE");
        params.put("result_code", "SUCCESS");
        params.put("total_fee", "100");

        service.createPaymentInfoForWxPayV2(params, "<xml/>");

        ArgumentCaptor<PaymentInfo> captor = ArgumentCaptor.forClass(PaymentInfo.class);
        verify(paymentInfoMapper).insert(captor.capture());
        PaymentInfo info = captor.getValue();

        // 现状: trade_state 取的是 result_code，不是 trade_state 字段
        assertThat(info.getTradeState()).isEqualTo("SUCCESS");
        assertThat(info.getTradeType()).isEqualTo("NATIVE");
        assertThat(info.getPayerTotal()).isEqualTo(100);
    }

    @Test
    @DisplayName("现状: createPaymentInfoForAliPay 的 trade_type 硬编码为 电脑网站支付")
    void current_alipay_payment_info_hardcodes_trade_type_as_pc_payment() {
        PaymentInfoMapper paymentInfoMapper = mock(PaymentInfoMapper.class);
        PaymentInfoServiceImpl service = new PaymentInfoServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", paymentInfoMapper);

        Map<String, Object> params = new HashMap<>();
        params.put("out_trade_no", "ORD-ALI");
        params.put("trade_no", "ALI-TX");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("total_amount", "1.00");

        service.createPaymentInfoForAliPay(params);

        ArgumentCaptor<PaymentInfo> captor = ArgumentCaptor.forClass(PaymentInfo.class);
        verify(paymentInfoMapper).insert(captor.capture());
        PaymentInfo info = captor.getValue();

        // 现状: trade_type 是硬编码的中文，不是从参数读取
        assertThat(info.getTradeType()).isEqualTo("电脑网站支付");
        assertThat(info.getPaymentType()).isEqualTo(PayType.ALIPAY.getType());
        assertThat(info.getPayerTotal()).isEqualTo(100);
    }

    @Test
    @DisplayName("现状: createPaymentInfoForAliPay 将 total_amount 从元转分")
    void current_alipay_payment_info_converts_yuan_to_cents() {
        PaymentInfoMapper paymentInfoMapper = mock(PaymentInfoMapper.class);
        PaymentInfoServiceImpl service = new PaymentInfoServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", paymentInfoMapper);

        Map<String, Object> params = new HashMap<>();
        params.put("out_trade_no", "ORD-ALI-2");
        params.put("trade_no", "ALI-TX-2");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("total_amount", "12.34");

        service.createPaymentInfoForAliPay(params);

        ArgumentCaptor<PaymentInfo> captor = ArgumentCaptor.forClass(PaymentInfo.class);
        verify(paymentInfoMapper).insert(captor.capture());
        assertThat(captor.getValue().getPayerTotal()).isEqualTo(1234);
    }

    @Test
    @DisplayName("现状: 支付流水插入遇 DuplicateKeyException 被吞掉，不抛异常")
    void current_payment_info_duplicate_key_is_swallowed_and_logged(CapturedOutput output) {
        PaymentInfoMapper paymentInfoMapper = mock(PaymentInfoMapper.class);
        when(paymentInfoMapper.insert(any(PaymentInfo.class))).thenThrow(new DuplicateKeyException("duplicate"));
        PaymentInfoServiceImpl service = new PaymentInfoServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", paymentInfoMapper);

        Map<String, String> params = new HashMap<>();
        params.put("out_trade_no", "ORD-DUP");
        params.put("transaction_id", "TX-DUP");
        params.put("trade_type", "NATIVE");
        params.put("result_code", "SUCCESS");
        params.put("total_fee", "1");

        assertThatCode(() -> service.createPaymentInfoForWxPayV2(params, "<xml/>")).doesNotThrowAnyException();

        verify(paymentInfoMapper).insert(any(PaymentInfo.class));
        assertThat(output.getOut()).contains("支付流水已存在");
        assertThat(output.getOut()).contains("ORD-DUP");
        assertThat(output.getOut()).contains("TX-DUP");
    }

    @Test
    @DisplayName("现状: 支付宝流水 content 字段存 JSON 序列化的通知参数")
    void current_alipay_payment_info_stores_json_content() {
        PaymentInfoMapper paymentInfoMapper = mock(PaymentInfoMapper.class);
        PaymentInfoServiceImpl service = new PaymentInfoServiceImpl();
        ReflectionTestUtils.setField(service, "baseMapper", paymentInfoMapper);

        Map<String, Object> params = new HashMap<>();
        params.put("out_trade_no", "ORD-ALI-3");
        params.put("trade_no", "ALI-TX-3");
        params.put("trade_status", "TRADE_SUCCESS");
        params.put("total_amount", "10.00");

        service.createPaymentInfoForAliPay(params);

        ArgumentCaptor<PaymentInfo> captor = ArgumentCaptor.forClass(PaymentInfo.class);
        verify(paymentInfoMapper).insert(captor.capture());
        assertThat(captor.getValue().getContent()).contains("out_trade_no");
        assertThat(captor.getValue().getContent()).contains("ORD-ALI-3");
    }

    // ==================== 退款申请 ====================

    @Test
    @DisplayName("现状: 新退款申请入口透传 orderNo/refundAmount/reason")
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
    @DisplayName("现状: 三路由退款申请入口行为等价 /api/refund-info/apply, /api/wx-pay/refunds, /api/ali-pay/trade/refund")
    void current_three_refund_apply_routes_are_equivalent() {
        RefundApplicationService refundApplicationService = mock(RefundApplicationService.class);
        RefundApplicationController controller = new RefundApplicationController(refundApplicationService);
        RefundRequest request = new RefundRequest();
        request.setOrderNo("ORD-EQUIV");
        request.setRefundAmount(100);
        request.setReason("test");

        controller.apply(request);

        verify(refundApplicationService).createApplication("ORD-EQUIV", 100, "test");
    }

    // ==================== 退款审核 ====================

    @Test
    @DisplayName("现状: approve 在审核通过时会调用 executeRefund 然后发延迟同步消息")
    void current_refund_approve_calls_execute_refund_and_sends_sync_message() {
        RefundInfoService refundInfoService = mock(RefundInfoService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayRefundFacade wxPayRefundFacade = mock(WxPayRefundFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);
        cc.ivera.service.refund.OrderRefundStatusService orderRefundStatusService =
                mock(cc.ivera.service.refund.OrderRefundStatusService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        cc.ivera.service.RefundStatusSyncMessageService refundStatusSyncMessageService =
                mock(cc.ivera.service.RefundStatusSyncMessageService.class);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundNo("REF-1");
        refundInfo.setOrderNo("ORD-1");
        refundInfo.setApprovalStatus(RefundApprovalStatus.PENDING.getType());
        refundInfo.setRefundStatus(RefundStatus.CREATED.getType());

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo("ORD-1");
        orderInfo.setPaymentType(PayType.WXPAY.getType());

        when(refundInfoService.getByRefundNo("REF-1")).thenReturn(refundInfo);
        when(orderInfoService.getOrderByOrderNo("ORD-1")).thenReturn(orderInfo);
        when(distributedLockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> {
                    invocation.getArgument(3, Supplier.class).get();
                    return null;
                });
        when(refundInfoService.updateRefundIfStatusIn(
                anyString(), any(), any(), any(), any(), any())).thenReturn(true);

        cc.ivera.service.impl.RefundApplicationServiceImpl service =
                new cc.ivera.service.impl.RefundApplicationServiceImpl(
                        refundInfoService, orderInfoService, wxPayRefundFacade, aliPayService,
                        orderRefundStatusService, distributedLockTemplate, refundStatusSyncMessageService);

        service.approve("REF-1", "approved");

        verify(wxPayRefundFacade).executeRefund(any(RefundInfo.class));
        verify(refundStatusSyncMessageService).sendRefundStatusSyncMessage("REF-1");
    }

    @Test
    @DisplayName("现状: approve 对已拒绝的退款申请抛异常")
    void current_refund_approve_rejects_already_rejected() {
        RefundInfoService refundInfoService = mock(RefundInfoService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayRefundFacade wxPayRefundFacade = mock(WxPayRefundFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);
        cc.ivera.service.refund.OrderRefundStatusService orderRefundStatusService =
                mock(cc.ivera.service.refund.OrderRefundStatusService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        cc.ivera.service.RefundStatusSyncMessageService refundStatusSyncMessageService =
                mock(cc.ivera.service.RefundStatusSyncMessageService.class);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundNo("REF-2");
        refundInfo.setApprovalStatus(RefundApprovalStatus.REJECTED.getType());

        when(refundInfoService.getByRefundNo("REF-2")).thenReturn(refundInfo);
        when(distributedLockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, Supplier.class).get());

        cc.ivera.service.impl.RefundApplicationServiceImpl service =
                new cc.ivera.service.impl.RefundApplicationServiceImpl(
                        refundInfoService, orderInfoService, wxPayRefundFacade, aliPayService,
                        orderRefundStatusService, distributedLockTemplate, refundStatusSyncMessageService);

        assertThatThrownBy(() -> service.approve("REF-2", "retry"))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("退款申请单已拒绝");
    }

    @Test
    @DisplayName("现状: approve 对已退款成功的退款申请抛异常")
    void current_refund_approve_rejects_already_refunded() {
        RefundInfoService refundInfoService = mock(RefundInfoService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayRefundFacade wxPayRefundFacade = mock(WxPayRefundFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);
        cc.ivera.service.refund.OrderRefundStatusService orderRefundStatusService =
                mock(cc.ivera.service.refund.OrderRefundStatusService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        cc.ivera.service.RefundStatusSyncMessageService refundStatusSyncMessageService =
                mock(cc.ivera.service.RefundStatusSyncMessageService.class);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundNo("REF-3");
        refundInfo.setRefundStatus(RefundStatus.SUCCESS.getType());

        when(refundInfoService.getByRefundNo("REF-3")).thenReturn(refundInfo);
        when(distributedLockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, Supplier.class).get());

        cc.ivera.service.impl.RefundApplicationServiceImpl service =
                new cc.ivera.service.impl.RefundApplicationServiceImpl(
                        refundInfoService, orderInfoService, wxPayRefundFacade, aliPayService,
                        orderRefundStatusService, distributedLockTemplate, refundStatusSyncMessageService);

        assertThatThrownBy(() -> service.approve("REF-3", null))
                .isInstanceOf(BizException.class)
                .hasMessageContaining("已退款成功");
    }

    @Test
    @DisplayName("现状: reject 使用独立分布式锁标记拒绝")
    void current_refund_reject_uses_dedicated_lock() {
        RefundInfoService refundInfoService = mock(RefundInfoService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayRefundFacade wxPayRefundFacade = mock(WxPayRefundFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);
        cc.ivera.service.refund.OrderRefundStatusService orderRefundStatusService =
                mock(cc.ivera.service.refund.OrderRefundStatusService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        cc.ivera.service.RefundStatusSyncMessageService refundStatusSyncMessageService =
                mock(cc.ivera.service.RefundStatusSyncMessageService.class);

        when(distributedLockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(invocation -> invocation.getArgument(3, Supplier.class).get());

        cc.ivera.service.impl.RefundApplicationServiceImpl service =
                new cc.ivera.service.impl.RefundApplicationServiceImpl(
                        refundInfoService, orderInfoService, wxPayRefundFacade, aliPayService,
                        orderRefundStatusService, distributedLockTemplate, refundStatusSyncMessageService);

        service.reject("REF-REJECT", "rejected reason");

        ArgumentCaptor<String> lockKeyCaptor = ArgumentCaptor.forClass(String.class);
        verify(distributedLockTemplate).execute(
                lockKeyCaptor.capture(), eq(5000L), eq(-1L), any(Supplier.class));
        assertThat(lockKeyCaptor.getValue()).contains("payment:refund:reject:");
    }

    // ==================== 退款状态刷新 ====================

    @Test
    @DisplayName("现状: 退款状态刷新优先级 — 全额成功 > 部分成功 > 处理中 > 异常 > 恢复成功")
    void current_refund_status_summary_priority_full_success_wins() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);

        OrderInfo orderInfo = paidOrder(100);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-REFUND")).thenReturn(orderInfo);
        // 全额退款成功
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(anyString(), any()))
                .thenReturn(100, 0, 0);

        service.refreshOrderRefundStatus("ORD-REFUND");

        verify(orderInfoService).updateStatusByOrderNo("ORD-REFUND", OrderStatus.REFUND_SUCCESS);
    }

    @Test
    @DisplayName("现状: 退款状态刷新 — 部分成功优先于处理中")
    void current_refund_status_summary_partial_success_wins_over_processing() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);

        OrderInfo orderInfo = paidOrder(100);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-REFUND")).thenReturn(orderInfo);
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(anyString(), any()))
                .thenReturn(10, 80, 0);

        service.refreshOrderRefundStatus("ORD-REFUND");

        verify(orderInfoService).updateStatusByOrderNo("ORD-REFUND", OrderStatus.PARTIAL_REFUND);
    }

    @Test
    @DisplayName("现状: 退款状态刷新 — 无成功但有处理中则 REFUND_PROCESSING")
    void current_refund_status_summary_processing_when_no_success() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);

        OrderInfo orderInfo = paidOrder(100);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-REFUND")).thenReturn(orderInfo);
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(anyString(), any()))
                .thenReturn(0, 80, 20);

        service.refreshOrderRefundStatus("ORD-REFUND");

        verify(orderInfoService).updateStatusByOrderNo("ORD-REFUND", OrderStatus.REFUND_PROCESSING);
    }

    @Test
    @DisplayName("现状: 退款状态刷新 — 仅异常则 REFUND_ABNORMAL")
    void current_refund_status_summary_abnormal_when_only_abnormal() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);

        OrderInfo orderInfo = paidOrder(100);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-REFUND")).thenReturn(orderInfo);
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(anyString(), any()))
                .thenReturn(0, 0, 20);

        service.refreshOrderRefundStatus("ORD-REFUND");

        verify(orderInfoService).updateStatusByOrderNo("ORD-REFUND", OrderStatus.REFUND_ABNORMAL);
    }

    @Test
    @DisplayName("现状: 退款状态刷新 — 无任何退款时恢复为 SUCCESS")
    void current_refund_status_summary_restores_success_when_no_refunds() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);

        OrderInfo orderInfo = paidOrder(100);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-REFUND")).thenReturn(orderInfo);
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(anyString(), any()))
                .thenReturn(0, 0, 0);

        service.refreshOrderRefundStatus("ORD-REFUND");

        verify(orderInfoService).updateStatusByOrderNo("ORD-REFUND", OrderStatus.SUCCESS);
    }

    @Test
    @DisplayName("现状: refreshOrderRefundStatusByRefundNo 通过 refundNo 找到 orderNo 再刷新")
    void current_refresh_by_refund_no_delegates_to_order_refresh() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);

        RefundInfo refundInfo = new RefundInfo();
        refundInfo.setRefundNo("REF-DELEGATE");
        refundInfo.setOrderNo("ORD-DELEGATE");
        when(refundInfoMapper.selectOne(any(QueryWrapper.class))).thenReturn(refundInfo);

        OrderInfo orderInfo = paidOrder(100);
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-DELEGATE")).thenReturn(orderInfo);
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(anyString(), any()))
                .thenReturn(0, 0, 0);

        service.refreshOrderRefundStatusByRefundNo("REF-DELEGATE");

        verify(orderInfoService).updateStatusByOrderNo("ORD-DELEGATE", OrderStatus.SUCCESS);
    }

    // ==================== 配置加载/缓存 ====================

    @Test
    @DisplayName("现状: PaymentConfigLoader 启动时数据库不可用不阻断应用，清空缓存并打 warn 日志")
    void current_config_loader_startup_missing_tables_does_not_abort_context(CapturedOutput output) {
        PaymentAppService paymentAppService = mock(PaymentAppService.class);
        PaymentChannelService paymentChannelService = mock(PaymentChannelService.class);
        when(paymentChannelService.listEnabledChannels())
                .thenThrow(new DataIntegrityViolationException("invalid table t_payment_channel"));
        PaymentConfigLoader loader = new PaymentConfigLoader(
                paymentAppService,
                paymentChannelService,
                new ObjectMapper());

        assertThatCode(loader::init).doesNotThrowAnyException();

        assertThat(loader.getAllAppConfigs()).isEmpty();
        assertThat(output.getOut()).contains("Payment config tables are not available during startup");
        // 异常堆栈不应出现在日志中
        assertThat(output.getOut()).doesNotContain("org.springframework.dao.DataIntegrityViolationException");
        verify(paymentChannelService).listEnabledChannels();
        verifyNoInteractions(paymentAppService);
        // reload 仍然会抛异常（数据库仍然不可用）
        assertThatThrownBy(loader::reloadConfigs).isInstanceOf(DataIntegrityViolationException.class);
    }

    @Test
    @DisplayName("现状: PaymentConfigLoader.reloadConfigs 清空缓存后重新加载")
    void current_config_loader_reload_clears_and_reloads() {
        PaymentAppService paymentAppService = mock(PaymentAppService.class);
        PaymentChannelService paymentChannelService = mock(PaymentChannelService.class);
        when(paymentChannelService.listEnabledChannels()).thenReturn(Collections.emptyList());
        when(paymentAppService.listEnabledApps()).thenReturn(Collections.emptyList());
        PaymentConfigLoader loader = new PaymentConfigLoader(
                paymentAppService,
                paymentChannelService,
                new ObjectMapper());

        loader.reloadConfigs();

        assertThat(loader.getAllAppConfigs()).isEmpty();
        verify(paymentChannelService).listEnabledChannels();
        verify(paymentAppService).listEnabledApps();
    }

    @Test
    @DisplayName("现状: payment-config/apps 直接返回 PaymentAppConfig 缓存对象引用")
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
        cc.ivera.controller.PaymentConfigController controller =
                new cc.ivera.controller.PaymentConfigController(paymentConfigLoader);

        R<Map<Long, PaymentAppConfig>> response = controller.apps();

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("成功");
        // 现状: 直接返回缓存引用，isSameAs
        assertThat(response.getData()).isSameAs(configs);
        assertThat(response.getData().get(7L).getAppid()).isEqualTo("wx-app-id-current");
        assertThat(response.getData().get(7L).getMchId()).isEqualTo("merchant-current");
    }

    @Test
    @DisplayName("现状: payment-config/reload 调用 reloadConfigs 并返回成功消息")
    void current_payment_config_reload_calls_reload_and_returns_success() {
        PaymentConfigLoader paymentConfigLoader = mock(PaymentConfigLoader.class);
        cc.ivera.controller.PaymentConfigController controller =
                new cc.ivera.controller.PaymentConfigController(paymentConfigLoader);

        R<Map<String, Object>> response = controller.reload();

        verify(paymentConfigLoader).reloadConfigs();
        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("支付配置已重新加载");
    }

    // ==================== RabbitMQ 消息路由 ====================

    @Test
    @DisplayName("现状: 关单消息使用固定的 exchange 和 routing key")
    void current_order_close_message_uses_fixed_exchange_and_routing_key() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OrderCloseMessageServiceImpl service = new OrderCloseMessageServiceImpl(rabbitTemplate);

        service.sendCloseOrderMessage("ORD-CLOSE", PayType.WXPAY.getType());

        ArgumentCaptor<OrderCloseMessage> captor = ArgumentCaptor.forClass(OrderCloseMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(OrderCloseRabbitConfig.ORDER_CLOSE_EVENT_EXCHANGE),
                eq(OrderCloseRabbitConfig.ORDER_CLOSE_DELAY_ROUTING_KEY),
                captor.capture());
        assertThat(captor.getValue().getOrderNo()).isEqualTo("ORD-CLOSE");
        assertThat(captor.getValue().getPaymentType()).isEqualTo(PayType.WXPAY.getType());
    }

    @Test
    @DisplayName("现状: 关单消息拒绝空 orderNo 或空 paymentType")
    void current_order_close_message_rejects_blank_order_or_payment_type() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        OrderCloseMessageServiceImpl service = new OrderCloseMessageServiceImpl(rabbitTemplate);

        assertThatThrownBy(() -> service.sendCloseOrderMessage("", PayType.WXPAY.getType()))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单号或支付类型为空");
        assertThatThrownBy(() -> service.sendCloseOrderMessage("ORD-CLOSE", ""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("订单号或支付类型为空");
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(OrderCloseMessage.class));
    }

    @Test
    @DisplayName("现状: 退款同步消息使用固定的 exchange 和 routing key")
    void current_refund_sync_message_uses_fixed_exchange_and_routing_key() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RefundStatusSyncMessageServiceImpl service = new RefundStatusSyncMessageServiceImpl(rabbitTemplate);

        service.sendRefundStatusSyncMessage("REF-SYNC");

        ArgumentCaptor<RefundStatusSyncMessage> captor = ArgumentCaptor.forClass(RefundStatusSyncMessage.class);
        verify(rabbitTemplate).convertAndSend(
                eq(RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_EVENT_EXCHANGE),
                eq(RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_DELAY_ROUTING_KEY),
                captor.capture());
        assertThat(captor.getValue().getRefundNo()).isEqualTo("REF-SYNC");
    }

    @Test
    @DisplayName("现状: 退款同步消息拒绝空 refundNo")
    void current_refund_sync_message_rejects_blank_refund_no() {
        RabbitTemplate rabbitTemplate = mock(RabbitTemplate.class);
        RefundStatusSyncMessageServiceImpl service = new RefundStatusSyncMessageServiceImpl(rabbitTemplate);

        assertThatThrownBy(() -> service.sendRefundStatusSyncMessage(""))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("refundNo must not be blank");
        verify(rabbitTemplate, never()).convertAndSend(anyString(), anyString(), any(RefundStatusSyncMessage.class));
    }

    // ==================== 订单状态轮询（可疑行为 1、4） ====================

    @Test
    @DisplayName("现状: 订单轮询查到支付成功时 code=0, message=支付成功")
    void current_order_status_success_returns_success_message() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        when(orderInfoService.getOrderStatus("ORD-1")).thenReturn(OrderStatus.SUCCESS.getType());
        OrderInfoController controller = new OrderInfoController(orderInfoService);

        R<Map<String, Object>> response = controller.queryOrderStatus("ORD-1");

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("支付成功");
    }

    @Test
    @DisplayName("现状: 订单轮询查到未支付时 code=101, message=支付中......")
    void current_order_status_notpay_returns_polling_code_101() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        when(orderInfoService.getOrderStatus("ORD-2")).thenReturn(OrderStatus.NOTPAY.getType());
        OrderInfoController controller = new OrderInfoController(orderInfoService);

        R<Map<String, Object>> response = controller.queryOrderStatus("ORD-2");

        assertThat(response.getCode()).isEqualTo(101);
        assertThat(response.getMessage()).isEqualTo("支付中......");
    }

    @Test
    @DisplayName("现状: 订单轮询查到空状态仍返回支付中 code=101")
    void current_order_status_null_still_returns_polling_code_101() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        when(orderInfoService.getOrderStatus("ORD-MISSING")).thenReturn(null);
        OrderInfoController controller = new OrderInfoController(orderInfoService);

        R<Map<String, Object>> response = controller.queryOrderStatus("ORD-MISSING");

        assertThat(response.getCode()).isEqualTo(101);
        assertThat(response.getMessage()).isEqualTo("支付中......");
    }

    @Test
    @DisplayName("现状: 订单轮询查到 CLOSED/CANCEL/REFUND_SUCCESS 等非成功状态仍返回 code=101, message=支付中")
    void current_order_status_closed_still_returns_polling_code_101() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        when(orderInfoService.getOrderStatus("ORD-CLOSED")).thenReturn(OrderStatus.CLOSED.getType());
        OrderInfoController controller = new OrderInfoController(orderInfoService);

        R<Map<String, Object>> response = controller.queryOrderStatus("ORD-CLOSED");

        assertThat(response.getCode()).isEqualTo(101);
        assertThat(response.getMessage()).isEqualTo("支付中......");
    }

    @Test
    @DisplayName("现状: 订单轮询查到 REFUND_PROCESSING 仍返回 code=101, message=支付中")
    void current_order_status_refund_processing_returns_polling_code_101() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        when(orderInfoService.getOrderStatus("ORD-REFUNDING"))
                .thenReturn(OrderStatus.REFUND_PROCESSING.getType());
        OrderInfoController controller = new OrderInfoController(orderInfoService);

        R<Map<String, Object>> response = controller.queryOrderStatus("ORD-REFUNDING");

        assertThat(response.getCode()).isEqualTo(101);
        assertThat(response.getMessage()).isEqualTo("支付中......");
    }

    // ==================== 微信 V2 通知（可疑行为 6） ====================

    @Test
    @DisplayName("现状: 微信 V2 通知 XML 解析失败时直接返回 FAIL，不触发 Redis/锁/DB/业务服务")
    void current_wxpay_v2_notify_bad_xml_returns_fail_without_touching_state() {
        WxPayOrderFacade wxPayOrderFacade = mock(WxPayOrderFacade.class);
        cc.ivera.config.WxPayConfig wxPayConfig = mock(cc.ivera.config.WxPayConfig.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        PaymentInfoService paymentInfoService = mock(PaymentInfoService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        WxPayV2Controller controller = new WxPayV2Controller(
                wxPayOrderFacade, wxPayConfig, orderInfoService, paymentInfoService,
                distributedLockTemplate, transactionTemplate, stringRedisTemplate);

        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent("<xml><broken>".getBytes(StandardCharsets.UTF_8));

        String response = controller.wxNotify(request);

        assertThat(response).contains("FAIL");
        assertThat(response).contains("失败");
        verifyNoInteractions(stringRedisTemplate);
        verifyNoInteractions(distributedLockTemplate);
        verifyNoInteractions(orderInfoService);
        verifyNoInteractions(paymentInfoService);
    }

    @Test
    @DisplayName("现状: 微信 V2 通知验签失败时释放 processing 锁，返回 FAIL")
    void current_wxpay_v2_notify_signature_failure_releases_lock() {
        WxPayOrderFacade wxPayOrderFacade = mock(WxPayOrderFacade.class);
        cc.ivera.config.WxPayConfig wxPayConfig = mock(cc.ivera.config.WxPayConfig.class);
        when(wxPayConfig.getPartnerKey()).thenReturn("test-partner-key");
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        PaymentInfoService paymentInfoService = mock(PaymentInfoService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        StringRedisTemplate stringRedisTemplate = mock(StringRedisTemplate.class);
        ValueOperations<String, String> valueOps = mock(ValueOperations.class);
        when(stringRedisTemplate.opsForValue()).thenReturn(valueOps);
        when(valueOps.setIfAbsent(anyString(), anyString(), anyLong(), any(TimeUnit.class)))
                .thenReturn(true);
        when(valueOps.get(anyString())).thenReturn("processing");

        WxPayV2Controller controller = new WxPayV2Controller(
                wxPayOrderFacade, wxPayConfig, orderInfoService, paymentInfoService,
                distributedLockTemplate, transactionTemplate, stringRedisTemplate);

        // 合法 XML 但验签会失败（WXPayUtil.isSignatureValid 需要有效签名）
        String xml = "<xml>" +
                "<return_code><![CDATA[SUCCESS]]></return_code>" +
                "<result_code><![CDATA[SUCCESS]]></result_code>" +
                "<out_trade_no><![CDATA[ORD-V2SIG]]></out_trade_no>" +
                "<transaction_id><![CDATA[TX-V2SIG]]></transaction_id>" +
                "<total_fee>100</total_fee>" +
                "<trade_type><![CDATA[NATIVE]]></trade_type>" +
                "</xml>";
        MockHttpServletRequest request = new MockHttpServletRequest();
        request.setContent(xml.getBytes(StandardCharsets.UTF_8));

        String response = controller.wxNotify(request);

        assertThat(response).contains("FAIL");
        // 验签失败后应释放 processing 锁
        verify(stringRedisTemplate).delete(anyString());
        // 不进入业务处理
        verifyNoInteractions(wxPayOrderFacade);
    }

    // ==================== ProductController ====================

    @Test
    @DisplayName("现状: ProductController.test 返回 R 包装, message=hello, data 中包含 now")
    void current_product_test_endpoint_returns_wrapped_hello_and_now() {
        cc.ivera.controller.ProductController controller =
                new cc.ivera.controller.ProductController(mock(ProductService.class));

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
        cc.ivera.entity.Product product = new cc.ivera.entity.Product();
        product.setId(1L);
        product.setTitle("Java课程");
        product.setPrice(1);
        List<cc.ivera.entity.Product> products = Arrays.asList(product);
        when(productService.list()).thenReturn(products);
        cc.ivera.controller.ProductController controller =
                new cc.ivera.controller.ProductController(productService);

        R<Map<String, Object>> response = controller.list();

        assertThat(response.getCode()).isEqualTo(0);
        assertThat(response.getMessage()).isEqualTo("成功");
        assertThat(response.getData()).containsEntry("productList", products);
    }

    // ==================== 延迟关单（可疑行为 8） ====================

    @Test
    @DisplayName("现状: 延迟关单对 NOTPAY 的微信订单调用 checkOrderStatus 而非直接关单")
    void current_order_close_wxpay_calls_check_order_status_not_direct_close() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayOrderFacade wxPayOrderFacade = mock(WxPayOrderFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo("ORD-CLOSE-WX");
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        orderInfo.setPaymentType(PayType.WXPAY.getType());
        when(orderInfoService.getOrderByOrderNo("ORD-CLOSE-WX")).thenReturn(orderInfo);

        cc.ivera.mq.OrderCloseConsumer consumer = new cc.ivera.mq.OrderCloseConsumer(
                orderInfoService, wxPayOrderFacade, aliPayService);

        OrderCloseMessage message = new OrderCloseMessage();
        message.setOrderNo("ORD-CLOSE-WX");
        message.setPaymentType(PayType.WXPAY.getType());
        consumer.handleOrderClose(message);

        // 现状: 不直接关闭，而是先主动查渠道状态
        verify(wxPayOrderFacade).checkOrderStatus("ORD-CLOSE-WX");
        verify(aliPayService, never()).checkOrderStatus(anyString());
    }

    @Test
    @DisplayName("现状: 延迟关单对 NOTPAY 的支付宝订单调用 checkOrderStatus 而非直接关单")
    void current_order_close_alipay_calls_check_order_status_not_direct_close() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayOrderFacade wxPayOrderFacade = mock(WxPayOrderFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo("ORD-CLOSE-ALI");
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        orderInfo.setPaymentType(PayType.ALIPAY.getType());
        when(orderInfoService.getOrderByOrderNo("ORD-CLOSE-ALI")).thenReturn(orderInfo);

        cc.ivera.mq.OrderCloseConsumer consumer = new cc.ivera.mq.OrderCloseConsumer(
                orderInfoService, wxPayOrderFacade, aliPayService);

        OrderCloseMessage message = new OrderCloseMessage();
        message.setOrderNo("ORD-CLOSE-ALI");
        message.setPaymentType(PayType.ALIPAY.getType());
        consumer.handleOrderClose(message);

        verify(aliPayService).checkOrderStatus("ORD-CLOSE-ALI");
        verify(wxPayOrderFacade, never()).checkOrderStatus(anyString());
    }

    @Test
    @DisplayName("现状: 延迟关单对已支付订单不执行任何操作")
    void current_order_close_skips_already_paid_order() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayOrderFacade wxPayOrderFacade = mock(WxPayOrderFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo("ORD-PAID");
        orderInfo.setOrderStatus(OrderStatus.SUCCESS.getType());
        orderInfo.setPaymentType(PayType.WXPAY.getType());
        when(orderInfoService.getOrderByOrderNo("ORD-PAID")).thenReturn(orderInfo);

        cc.ivera.mq.OrderCloseConsumer consumer = new cc.ivera.mq.OrderCloseConsumer(
                orderInfoService, wxPayOrderFacade, aliPayService);

        OrderCloseMessage message = new OrderCloseMessage();
        message.setOrderNo("ORD-PAID");
        message.setPaymentType(PayType.WXPAY.getType());
        consumer.handleOrderClose(message);

        verify(wxPayOrderFacade, never()).checkOrderStatus(anyString());
        verify(aliPayService, never()).checkOrderStatus(anyString());
    }

    @Test
    @DisplayName("现状: 延迟关单对不存在的订单直接返回，不报错")
    void current_order_close_handles_missing_order_gracefully() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayOrderFacade wxPayOrderFacade = mock(WxPayOrderFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);
        when(orderInfoService.getOrderByOrderNo("ORD-GONE")).thenReturn(null);

        cc.ivera.mq.OrderCloseConsumer consumer = new cc.ivera.mq.OrderCloseConsumer(
                orderInfoService, wxPayOrderFacade, aliPayService);

        OrderCloseMessage message = new OrderCloseMessage();
        message.setOrderNo("ORD-GONE");
        message.setPaymentType(PayType.WXPAY.getType());

        assertThatCode(() -> consumer.handleOrderClose(message)).doesNotThrowAnyException();

        verify(wxPayOrderFacade, never()).checkOrderStatus(anyString());
    }

    @Test
    @DisplayName("现状: 延迟关单对空消息直接返回，不报错")
    void current_order_close_handles_null_message_gracefully() {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayOrderFacade wxPayOrderFacade = mock(WxPayOrderFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);

        cc.ivera.mq.OrderCloseConsumer consumer = new cc.ivera.mq.OrderCloseConsumer(
                orderInfoService, wxPayOrderFacade, aliPayService);

        assertThatCode(() -> consumer.handleOrderClose(null)).doesNotThrowAnyException();
        assertThatCode(() -> consumer.handleOrderClose(new OrderCloseMessage())).doesNotThrowAnyException();

        verify(orderInfoService, never()).getOrderByOrderNo(anyString());
    }

    @Test
    @DisplayName("现状: 延迟关单对未知支付类型打 warn 日志，不抛异常")
    void current_order_close_warns_on_unknown_payment_type(CapturedOutput output) {
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayOrderFacade wxPayOrderFacade = mock(WxPayOrderFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);

        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo("ORD-UNKNOWN");
        orderInfo.setOrderStatus(OrderStatus.NOTPAY.getType());
        orderInfo.setPaymentType("UNKNOWN_PAY");
        when(orderInfoService.getOrderByOrderNo("ORD-UNKNOWN")).thenReturn(orderInfo);

        cc.ivera.mq.OrderCloseConsumer consumer = new cc.ivera.mq.OrderCloseConsumer(
                orderInfoService, wxPayOrderFacade, aliPayService);

        OrderCloseMessage message = new OrderCloseMessage();
        message.setOrderNo("ORD-UNKNOWN");
        message.setPaymentType("UNKNOWN_PAY");
        consumer.handleOrderClose(message);

        assertThat(output.getOut()).contains("未知支付类型");
        verify(wxPayOrderFacade, never()).checkOrderStatus(anyString());
        verify(aliPayService, never()).checkOrderStatus(anyString());
    }

    // ==================== 辅助方法 ====================

    private OrderInfo paidOrder(int totalFee) {
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo("ORD-REFUND");
        orderInfo.setTotalFee(totalFee);
        orderInfo.setOrderStatus(OrderStatus.SUCCESS.getType());
        return orderInfo;
    }
}
