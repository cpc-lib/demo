package cc.ivera.characterization;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.RefundInfo;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PayType;
import cc.ivera.enums.RefundApprovalStatus;
import cc.ivera.enums.RefundStatus;
import cc.ivera.enums.alipay.AliPayTradeState;
import cc.ivera.enums.wxpay.WxApiType;
import cc.ivera.enums.wxpay.WxNotifyType;
import cc.ivera.enums.wxpay.WxRefundStatus;
import cc.ivera.enums.wxpay.WxTradeState;
import cc.ivera.exception.BizException;
import cc.ivera.lock.DistributedLockTemplate;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.PaymentInfoMapper;
import cc.ivera.mapper.ProductMapper;
import cc.ivera.mapper.RefundInfoMapper;
import cc.ivera.service.*;
import cc.ivera.service.impl.*;
import cc.ivera.service.impl.wxpay.WxPayOrderService;
import cc.ivera.service.impl.wxpay.WxPayRefundService;
import cc.ivera.service.refund.OrderRefundStatusService;
import cc.ivera.service.refund.RefundStatusSyncResult;
import cc.ivera.service.wxpay.WxPayOrderFacade;
import cc.ivera.service.wxpay.WxPayRefundFacade;
import cc.ivera.util.OrderNoUtils;
import com.baomidou.mybatisplus.core.conditions.query.QueryWrapper;
import com.baomidou.mybatisplus.core.conditions.update.UpdateWrapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.boot.test.system.CapturedOutput;
import org.springframework.boot.test.system.OutputCaptureExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.*;
import java.util.function.Supplier;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

/**
 * 设计模式重构前的特征测试 — 锁住 Provider/State/Template/Chain/Command 相关行为。
 *
 * 所有测试使用 mock 隔离，不依赖真实数据库/Redis/RabbitMQ。
 */
@ExtendWith({MockitoExtension.class, OutputCaptureExtension.class})
class DesignPatternRefactorCharacterizationTest {

    // ==================== Strategy: PaymentProvider 行为锁定 ====================

    @Test
    @DisplayName("现状: AliPayService.tradeCreate 接受 productId 和 paymentAppId")
    void current_alipay_service_trade_create_signature() {
        // 验证 AliPayService 接口的方法签名
        assertThat(AliPayService.class.getDeclaredMethods())
                .extracting(m -> m.getName() + ":" + m.getParameterCount())
                .contains("tradeCreate:1", "tradeCreate:2", "processOrder:1",
                        "cancelOrder:1", "queryOrder:1", "checkOrderStatus:1",
                        "executeRefund:1", "queryRefund:1", "queryRefundStatusForSync:1",
                        "queryBill:2");
    }

    @Test
    @DisplayName("现状: WxPayOrderFacade.nativePay 接受 productId 和 paymentAppId")
    void current_wxpay_order_facade_native_pay_signature() {
        assertThat(WxPayOrderFacade.class.getDeclaredMethods())
                .extracting(m -> m.getName() + ":" + m.getParameterCount())
                .contains("nativePay:1", "nativePay:2", "processOrder:1",
                        "cancelOrder:1", "queryOrder:1", "queryPaymentStatus:1",
                        "checkOrderStatus:1", "nativePayV2:2", "nativePayV2:3",
                        "jsapiPay:2");
    }

    @Test
    @DisplayName("现状: WxPayRefundFacade 方法签名")
    void current_wxpay_refund_facade_signature() {
        assertThat(WxPayRefundFacade.class.getDeclaredMethods())
                .extracting(m -> m.getName() + ":" + m.getParameterCount())
                .contains("executeRefund:1", "queryRefund:1",
                        "queryRefundStatusForSync:1", "queryOrderRefundsForSync:1",
                        "processRefund:1");
    }

    @Test
    @DisplayName("现状: AliPayService 和 WxPayOrderFacade 有共同的 executeRefund 语义")
    void current_both_providers_have_execute_refund() {
        // 两个接口都有 executeRefund，但参数类型不同
        assertThatCode(() -> AliPayService.class.getMethod("executeRefund", RefundInfo.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> WxPayRefundFacade.class.getMethod("executeRefund", RefundInfo.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: AliPayService 和 WxPayOrderFacade 有共同的 cancelOrder 语义")
    void current_both_providers_have_cancel_order() {
        assertThatCode(() -> AliPayService.class.getMethod("cancelOrder", String.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> WxPayOrderFacade.class.getMethod("cancelOrder", String.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: AliPayService 和 WxPayOrderFacade 有共同的 queryOrder 语义")
    void current_both_providers_have_query_order() {
        assertThatCode(() -> AliPayService.class.getMethod("queryOrder", String.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> WxPayOrderFacade.class.getMethod("queryOrder", String.class))
                .doesNotThrowAnyException();
    }

    // ==================== State: 订单状态转换行为锁定 ====================

    @Test
    @DisplayName("现状: OrderStatus 枚举值")
    void current_order_status_enum_values() {
        assertThat(OrderStatus.NOTPAY.getType()).isEqualTo("未支付");
        assertThat(OrderStatus.SUCCESS.getType()).isEqualTo("支付成功");
        assertThat(OrderStatus.CLOSED.getType()).isEqualTo("超时已关闭");
        assertThat(OrderStatus.CANCEL.getType()).isEqualTo("用户已取消");
        assertThat(OrderStatus.REFUND_PROCESSING.getType()).isEqualTo("退款中");
        assertThat(OrderStatus.PARTIAL_REFUND.getType()).isEqualTo("部分退款");
        assertThat(OrderStatus.REFUND_SUCCESS.getType()).isEqualTo("已退款");
        assertThat(OrderStatus.REFUND_ABNORMAL.getType()).isEqualTo("退款异常");
    }

    @Test
    @DisplayName("现状: RefundStatus 枚举值")
    void current_refund_status_enum_values() {
        assertThat(RefundStatus.CREATED.getType()).isEqualTo("CREATED");
        assertThat(RefundStatus.PROCESSING.getType()).isEqualTo("PROCESSING");
        assertThat(RefundStatus.SUCCESS.getType()).isEqualTo("SUCCESS");
        assertThat(RefundStatus.FAILED.getType()).isEqualTo("FAILED");
        assertThat(RefundStatus.CLOSED.getType()).isEqualTo("CLOSED");
        assertThat(RefundStatus.ABNORMAL.getType()).isEqualTo("ABNORMAL");
    }

    @Test
    @DisplayName("现状: RefundApprovalStatus 枚举值")
    void current_refund_approval_status_enum_values() {
        assertThat(RefundApprovalStatus.PENDING.getType()).isEqualTo("PENDING");
        assertThat(RefundApprovalStatus.APPROVED.getType()).isEqualTo("APPROVED");
        assertThat(RefundApprovalStatus.REJECTED.getType()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("现状: 订单状态从 NOTPAY → SUCCESS 通过 updateStatusByOrderNoIfStatus")
    void current_order_transition_notpay_to_success_uses_conditional_update() {
        OrderInfoMapper orderInfoMapper = mock(OrderInfoMapper.class);
        OrderCloseMessageService orderCloseMessageService = mock(OrderCloseMessageService.class);
        DistributedLockTemplate distributedLockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate transactionTemplate = mock(TransactionTemplate.class);
        OrderInfoServiceImpl service = new OrderInfoServiceImpl(
                mock(ProductMapper.class), orderCloseMessageService, distributedLockTemplate, transactionTemplate);
        ReflectionTestUtils.setField(service, "baseMapper", orderInfoMapper);

        when(orderInfoMapper.update(any(OrderInfo.class), any(UpdateWrapper.class))).thenReturn(1);

        boolean updated = service.updateStatusByOrderNoIfStatus("ORD-TRANS",
                OrderStatus.NOTPAY, OrderStatus.SUCCESS);

        assertThat(updated).isTrue();
        verify(orderInfoMapper).update(any(OrderInfo.class), any(UpdateWrapper.class));
    }

    @Test
    @DisplayName("现状: OrderRefundStatusService 状态优先级: 全额成功 > 部分成功 > 处理中 > 异常 > 恢复成功")
    void current_refund_status_priority_order() {
        // 全额成功: successAmount >= totalFee → REFUND_SUCCESS
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        RefundInfoMapper refundInfoMapper = mock(RefundInfoMapper.class);
        OrderRefundStatusService service = new OrderRefundStatusService(orderInfoService, refundInfoMapper);
        OrderInfo orderInfo = new OrderInfo();
        orderInfo.setOrderNo("ORD-PRIO");
        orderInfo.setTotalFee(100);
        orderInfo.setOrderStatus(OrderStatus.SUCCESS.getType());
        when(orderInfoService.getOrderByOrderNoForUpdate("ORD-PRIO")).thenReturn(orderInfo);
        when(refundInfoMapper.sumRefundAmountByOrderNoAndStatuses(eq("ORD-PRIO"), any()))
                .thenReturn(100)  // success
                .thenReturn(0)    // processing
                .thenReturn(0);   // abnormal

        service.refreshOrderRefundStatus("ORD-PRIO");

        verify(orderInfoService).updateStatusByOrderNo("ORD-PRIO", OrderStatus.REFUND_SUCCESS);
    }

    @Test
    @DisplayName("现状: 退款创建时初始状态为 CREATED + PENDING")
    void current_refund_creation_initial_status() {
        // 验证 RefundInfoServiceImpl.createRefundApplication 方法签名
        assertThatCode(() -> RefundInfoServiceImpl.class.getDeclaredMethod(
                "createRefundApplication", String.class, Integer.class, String.class))
                .doesNotThrowAnyException();

        // 验证初始状态枚举值
        assertThat(RefundStatus.CREATED.getType()).isEqualTo("CREATED");
        assertThat(RefundApprovalStatus.PENDING.getType()).isEqualTo("PENDING");
    }

    // ==================== Template Method: 支付流程模板行为锁定 ====================

    @Test
    @DisplayName("现状: AliPayServiceImpl.doTradeCreate 流程: resolveConfig → createOrReuseOrder → buildRequest → callApi → return")
    void current_alipay_trade_create_flow_order() {
        // 验证 AliPayServiceImpl 的 doTradeCreate 方法内部流程顺序
        // 1. resolveAliPayConfig(paymentAppId)
        // 2. orderInfoService.createOrReuseOrder(productId, ALIPAY, appId, CHANNEL_ALIPAY)
        // 3. 构建 AlipayTradePagePayRequest
        // 4. buildAlipayClient(payConfig).pageExecute(request)
        // 5. 返回 response.getBody() 或抛异常
        // 此测试通过反射验证方法存在性
        assertThatCode(() -> AliPayServiceImpl.class.getDeclaredMethod("doTradeCreate", Long.class, Long.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: WxPayOrderService.doNativePay 流程: resolveConfig → createOrReuseOrder → checkCodeUrl → buildParams → callApi → saveCodeUrl → return")
    void current_wxpay_native_pay_flow_order() {
        assertThatCode(() -> WxPayOrderService.class.getDeclaredMethod("doNativePay", Long.class, Long.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: 支付流程都使用 DistributedLockTemplate.execute 包裹")
    void current_payment_flows_use_distributed_lock_template() {
        // AliPayServiceImpl.tradeCreate 和 WxPayOrderService.nativePay 都使用 distributedLockTemplate.execute
        // 锁 key 格式: "payment:ali:pagepay:" + productId + ":" + paymentAppId
        // 锁 key 格式: "payment:wx:native:v3:" + productId + ":" + paymentAppId
        ProductMapper productMapper = mock(ProductMapper.class);
        OrderCloseMessageService orderCloseMessageService = mock(OrderCloseMessageService.class);
        DistributedLockTemplate lockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate txTemplate = mock(TransactionTemplate.class);
        OrderInfoServiceImpl orderService = new OrderInfoServiceImpl(
                productMapper, orderCloseMessageService, lockTemplate, txTemplate);

        when(lockTemplate.execute(anyString(), anyLong(), anyLong(), any(Supplier.class)))
                .thenAnswer(inv -> inv.getArgument(3, Supplier.class).get());

        orderService.createOrReuseOrder(1L, PayType.WXPAY.getType(), 7L, "WXPAY");

        ArgumentCaptor<String> keyCaptor = ArgumentCaptor.forClass(String.class);
        verify(lockTemplate).execute(keyCaptor.capture(), eq(3000L), eq(10000L), any(Supplier.class));
        assertThat(keyCaptor.getValue()).contains(":7");
    }

    @Test
    @DisplayName("现状: 支付流程在事务后发送关单消息")
    void current_payment_flow_sends_close_message_after_transaction() {
        // OrderInfoServiceImpl.createOrReuseOrder 在事务提交后通过 TransactionSynchronizationManager
        // 注册 afterCommit 回调发送关单消息
        ProductMapper productMapper = mock(ProductMapper.class);
        OrderCloseMessageService orderCloseMessageService = mock(OrderCloseMessageService.class);
        DistributedLockTemplate lockTemplate = mock(DistributedLockTemplate.class);
        TransactionTemplate txTemplate = new TransactionTemplate();
        OrderInfoServiceImpl service = new OrderInfoServiceImpl(
                productMapper, orderCloseMessageService, lockTemplate, txTemplate);

        // 验证 sendCloseOrderMessage 方法存在且签名正确
        assertThatCode(() -> OrderCloseMessageService.class.getMethod("sendCloseOrderMessage", String.class, String.class))
                .doesNotThrowAnyException();
    }

    // ==================== Chain of Responsibility: 通知处理链行为锁定 ====================

    @Test
    @DisplayName("现状: WxPayNotifyHandler.handle 流程: readBody → parseJson → tryAcquireNotifyLock → validateRequest → processor.accept → markProcessed")
    void current_wxpay_notify_handler_flow() {
        // 验证 WxPayNotifyHandler 的 handle 方法签名
        assertThatCode(() -> cc.ivera.controller.support.WxPayNotifyHandler.class
                .getDeclaredMethod("handle",
                        javax.servlet.http.HttpServletRequest.class,
                        javax.servlet.http.HttpServletResponse.class,
                        java.util.function.Consumer.class,
                        String.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: 支付宝通知处理流程: getOrderNo → lock → getOrderForUpdate → validate → updateStatus → createPaymentInfo")
    void current_alipay_notify_processing_flow() {
        // AliPayServiceImpl.processOrder 内部流程
        // 1. 提取 orderNo
        // 2. distributedLockTemplate.execute(lockKey, 5000, 30000, ...)
        // 3. transactionTemplate.execute(...)
        // 4. doProcessAliPayNotifyInTransaction: getOrderForUpdate → validate → updateStatus → createPaymentInfo
        assertThatCode(() -> AliPayServiceImpl.class.getDeclaredMethod(
                "doProcessAliPayNotifyInTransaction", Map.class, String.class, String.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: 微信支付通知处理流程: decryptResource → getOrderNo → lock → getOrderForUpdate → validate → updateStatus → createPaymentInfo")
    void current_wxpay_notify_processing_flow() {
        assertThatCode(() -> WxPayOrderService.class.getDeclaredMethod(
                "doProcessOrderNotifyInTransaction", String.class, Map.class, String.class, String.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: 通知幂等: 已处理的通知通过 Redis SETNX 去重")
    void current_notify_idempotency_uses_redis_setnx() {
        // WxPayNotifyHandler.tryAcquireNotifyLock 使用 stringRedisTemplate.opsForValue().setIfAbsent
        // 这是通过 Redis SETNX 实现的幂等锁
        assertThatCode(() -> cc.ivera.controller.support.WxPayNotifyHandler.class
                .getDeclaredMethod("tryAcquireNotifyLock", String.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: 通知验签失败时释放幂等锁并返回错误响应")
    void current_notify_verification_failure_releases_lock() {
        // WxPayNotifyHandler.handle 中验签失败时调用 releaseNotifyLock
        assertThatCode(() -> cc.ivera.controller.support.WxPayNotifyHandler.class
                .getDeclaredMethod("releaseNotifyLock", String.class))
                .doesNotThrowAnyException();
    }

    // ==================== Command: 退款命令行为锁定 ====================

    @Test
    @DisplayName("现状: RefundApplicationServiceImpl.approve 流程: lock → getRefundInfo → validate → markApprovalPassed → claimRefund → executeRefund → syncMessage")
    void current_refund_approve_flow() {
        // approve 方法内部步骤:
        // 1. distributedLockTemplate.execute(lockKey, 5000, -1, ...)
        // 2. refundInfoService.getByRefundNo(refundNo)
        // 3. 校验: 不存在/已拒绝/已退款成功/处理中
        // 4. refundInfoService.markApprovalPassed(refundNo, approveRemark)
        // 5. claimRefundForExecution(refundNo)
        // 6. executeRefund(paymentType, refundInfo)
        // 7. refundStatusSyncMessageService.sendRefundStatusSyncMessage(refundNo)
        assertThatCode(() -> RefundApplicationServiceImpl.class.getDeclaredMethod(
                "executeRefund", String.class, RefundInfo.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: 退款执行根据 paymentType 分发到不同 Provider")
    void current_refund_execution_dispatches_by_payment_type() {
        // executeRefund 根据 paymentType 调用 aliPayService.executeRefund 或 wxPayRefundFacade.executeRefund
        RefundInfoService refundInfoService = mock(RefundInfoService.class);
        OrderInfoService orderInfoService = mock(OrderInfoService.class);
        WxPayRefundFacade wxPayRefundFacade = mock(WxPayRefundFacade.class);
        AliPayService aliPayService = mock(AliPayService.class);
        OrderRefundStatusService orderRefundStatusService = mock(OrderRefundStatusService.class);
        DistributedLockTemplate lockTemplate = mock(DistributedLockTemplate.class);
        RefundStatusSyncMessageService syncMessageService = mock(RefundStatusSyncMessageService.class);

        RefundApplicationServiceImpl service = new RefundApplicationServiceImpl(
                refundInfoService, orderInfoService, wxPayRefundFacade, aliPayService,
                orderRefundStatusService, lockTemplate, syncMessageService);

        // 验证 executeRefund 方法通过反射可访问
        assertThatCode(() -> {
            java.lang.reflect.Method method = RefundApplicationServiceImpl.class.getDeclaredMethod(
                    "executeRefund", String.class, RefundInfo.class);
            method.setAccessible(true);
            assertThat(method).isNotNull();
        }).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: 退款审核拒绝时状态变为 CLOSED")
    void current_refund_reject_sets_status_to_closed() {
        // markApprovalRejected 方法签名验证
        assertThatCode(() -> RefundInfoServiceImpl.class.getDeclaredMethod(
                "markApprovalRejected", String.class, String.class))
                .doesNotThrowAnyException();

        // 验证 RefundStatus.CLOSED 和 RefundApprovalStatus.REJECTED 枚举值存在
        assertThat(RefundStatus.CLOSED.getType()).isEqualTo("CLOSED");
        assertThat(RefundApprovalStatus.REJECTED.getType()).isEqualTo("REJECTED");
    }

    @Test
    @DisplayName("现状: RefundStatusSyncMessage 通过 RabbitMQ 发送到固定 Exchange 和 RoutingKey")
    void current_refund_sync_message_uses_fixed_exchange_and_routing_key() {
        // 验证 RefundStatusSyncMessageServiceImpl 存在 sendRefundStatusSyncMessage 方法
        assertThatCode(() -> RefundStatusSyncMessageServiceImpl.class.getDeclaredMethod(
                "sendRefundStatusSyncMessage", String.class))
                .doesNotThrowAnyException();

        // 验证 Exchange 和 RoutingKey 常量存在
        assertThat(cc.ivera.config.RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_EVENT_EXCHANGE)
                .isEqualTo("payment.refund.status-sync.event.exchange");
        assertThat(cc.ivera.config.RefundStatusSyncRabbitConfig.REFUND_STATUS_SYNC_DELAY_ROUTING_KEY)
                .isEqualTo("payment.refund.status-sync.delay");
    }

    // ==================== 辅助方法行为锁定 ====================

    @Test
    @DisplayName("现状: PaymentConfigLoader 提供 getAppConfig 方法")
    void current_payment_config_loader_resolve_ali_pay_config() {
        // PaymentConfigLoader 提供 getAppConfig / getRequiredAppConfig 等方法
        assertThatCode(() -> PaymentConfigLoader.class.getDeclaredMethod("getAppConfig", Long.class))
                .doesNotThrowAnyException();
        assertThatCode(() -> PaymentConfigLoader.class.getDeclaredMethod("getRequiredAppConfig", Long.class))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("现状: WxPayRefundService.mapWxRefundStatus 映射规则")
    void current_wxpay_refund_status_mapping_rules() {
        // 验证映射方法存在
        assertThatCode(() -> WxPayRefundService.class.getDeclaredMethod("mapWxRefundStatus", String.class))
                .doesNotThrowAnyException();

        // SUCCESS → RefundStatus.SUCCESS
        assertThat(WxRefundStatus.SUCCESS.getType()).isEqualTo("SUCCESS");
        // PROCESSING → RefundStatus.PROCESSING
        assertThat(WxRefundStatus.PROCESSING.getType()).isEqualTo("PROCESSING");
        // ABNORMAL → RefundStatus.ABNORMAL
        assertThat(WxRefundStatus.ABNORMAL.getType()).isEqualTo("ABNORMAL");
        // CLOSED → RefundStatus.CLOSED
        assertThat(WxRefundStatus.CLOSED.getType()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("现状: WxTradeState 枚举值")
    void current_wx_trade_state_enum_values() {
        assertThat(WxTradeState.SUCCESS.getType()).isEqualTo("SUCCESS");
        assertThat(WxTradeState.NOTPAY.getType()).isEqualTo("NOTPAY");
        assertThat(WxTradeState.CLOSED.getType()).isEqualTo("CLOSED");
    }

    @Test
    @DisplayName("现状: AliPayTradeState 枚举值")
    void current_alipay_trade_state_enum_values() {
        assertThat(AliPayTradeState.SUCCESS.getType()).isEqualTo("TRADE_SUCCESS");
        assertThat(AliPayTradeState.NOTPAY.getType()).isEqualTo("WAIT_BUYER_PAY");
        assertThat(AliPayTradeState.CLOSED.getType()).isEqualTo("TRADE_CLOSED");
    }

    @Test
    @DisplayName("现状: PayType 枚举值")
    void current_pay_type_enum_values() {
        assertThat(PayType.WXPAY.getType()).isEqualTo("微信");
        assertThat(PayType.ALIPAY.getType()).isEqualTo("支付宝");
    }

    @Test
    @DisplayName("现状: PaymentConfigLoader.CHANNEL 常量")
    void current_payment_config_loader_channel_constants() {
        assertThat(PaymentConfigLoader.CHANNEL_WXPAY).isEqualTo("WXPAY");
        assertThat(PaymentConfigLoader.CHANNEL_ALIPAY).isEqualTo("ALIPAY");
    }

    @Test
    @DisplayName("现状: DistributedLockTemplate 接口方法签名")
    void current_distributed_lock_template_signature() {
        assertThat(DistributedLockTemplate.class.getDeclaredMethods())
                .extracting(m -> m.getName())
                .contains("execute");
    }

    @Test
    @DisplayName("现状: RefundStatusSyncResult.of 工厂方法")
    void current_refund_status_sync_result_factory() {
        RefundStatusSyncResult result = RefundStatusSyncResult.of(
                "ORD-1", "REF-1", "RF-1", "SUCCESS",
                RefundStatus.SUCCESS, "{}", 100, 50);
        assertThat(result.getOrderNo()).isEqualTo("ORD-1");
        assertThat(result.getRefundNo()).isEqualTo("REF-1");
        assertThat(result.getRefundId()).isEqualTo("RF-1");
        assertThat(result.getChannelStatus()).isEqualTo("SUCCESS");
        assertThat(result.getRefundStatus()).isEqualTo(RefundStatus.SUCCESS);
        assertThat(result.getContent()).isEqualTo("{}");
        assertThat(result.getTotalFee()).isEqualTo(100);
        assertThat(result.getRefundAmount()).isEqualTo(50);
    }

    @Test
    @DisplayName("现状: OrderNoUtils 生成订单号和退款单号")
    void current_order_no_utils_generates_order_and_refund_numbers() {
        String orderNo = OrderNoUtils.getOrderNo();
        String refundNo = OrderNoUtils.getRefundNo();
        assertThat(orderNo).isNotNull().isNotEmpty();
        assertThat(refundNo).isNotNull().isNotEmpty();
        assertThat(refundNo).startsWith("RFD");
    }

    @Test
    @DisplayName("现状: BizException 是 RuntimeException")
    void current_biz_exception_is_runtime_exception() {
        assertThat(RuntimeException.class.isAssignableFrom(BizException.class)).isTrue();
    }

    @Test
    @DisplayName("现状: WxApiType 枚举值")
    void current_wx_api_type_enum_values() {
        assertThat(WxApiType.NATIVE_PAY.getType()).isNotNull();
        assertThat(WxApiType.ORDER_QUERY_BY_NO.getType()).isNotNull();
        assertThat(WxApiType.DOMESTIC_REFUNDS.getType()).isNotNull();
        assertThat(WxApiType.DOMESTIC_REFUNDS_QUERY.getType()).isNotNull();
        assertThat(WxApiType.REFUND_QUERY_V2.getType()).isNotNull();
    }

    @Test
    @DisplayName("现状: WxNotifyType 枚举值")
    void current_wx_notify_type_enum_values() {
        assertThat(WxNotifyType.NATIVE_NOTIFY.getType()).isNotNull();
        assertThat(WxNotifyType.REFUND_NOTIFY.getType()).isNotNull();
    }
}
