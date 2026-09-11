package cc.ivera.service.payment;

import cc.ivera.config.PaymentAppConfig;
import cc.ivera.config.PaymentConfigLoader;
import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentOrder;
import cc.ivera.enums.OrderStatus;
import cc.ivera.enums.PaymentDisposition;
import cc.ivera.enums.PaymentOrderStatus;
import cc.ivera.enums.UserRole;
import cc.ivera.exception.BizException;
import cc.ivera.exception.ConflictException;
import cc.ivera.exception.ForbiddenException;
import cc.ivera.exception.NotFoundException;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.mapper.PaymentOrderMapper;
import cc.ivera.security.AuthUser;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.Date;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.UUID;

@Service
public class PaymentAttemptServiceImpl implements PaymentAttemptService {

    static final long MIN_REMAINING_MILLIS = 60_000L;

    private final OrderInfoMapper orderInfoMapper;

    private final PaymentOrderMapper paymentOrderMapper;

    private final PaymentConfigLoader paymentConfigLoader;

    public PaymentAttemptServiceImpl(
            OrderInfoMapper orderInfoMapper,
            PaymentOrderMapper paymentOrderMapper,
            PaymentConfigLoader paymentConfigLoader
    ) {
        this.orderInfoMapper = orderInfoMapper;
        this.paymentOrderMapper = paymentOrderMapper;
        this.paymentConfigLoader = paymentConfigLoader;
    }

    @Override
    @Transactional
    public PaymentOrder createOrReplay(
            String orderNo,
            Long paymentAppId,
            String protocol,
            String requestId,
            AuthUser requester
    ) {
        requireShoppingUser(requester);
        String normalizedOrderNo = requireText(orderNo, 64, "订单号无效");
        String normalizedProtocol = requireText(protocol, 32, "支付协议无效")
                .toUpperCase(Locale.ROOT);
        String normalizedRequestId = requireText(requestId, 64, "支付请求号无效");
        if (paymentAppId == null) {
            throw new ConflictException("支付应用不能为空");
        }

        OrderInfo order = orderInfoMapper.selectByOrderNoForUpdate(normalizedOrderNo);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }
        if (!Objects.equals(requester.getUserId(), order.getUserId())) {
            throw new ForbiddenException("无权为该订单创建支付尝试");
        }

        PaymentOrder existing = paymentOrderMapper.selectByOrderNoAndRequestIdForUpdate(
                normalizedOrderNo,
                normalizedRequestId
        );
        if (existing != null) {
            validateReplay(existing, order, paymentAppId, normalizedProtocol);
            return existing;
        }

        validatePayable(order);
        PaymentOrder uncertain = paymentOrderMapper.selectUncertainByOrderNoForUpdate(normalizedOrderNo);
        if (uncertain != null) {
            throw new ConflictException("存在状态未确认的支付尝试，请先查询支付单："
                    + uncertain.getPaymentNo());
        }

        PaymentAppConfig appConfig = paymentConfigLoader.getRequiredAppConfig(paymentAppId);
        String channel = normalizeChannel(appConfig.getChannelCode());
        validateProtocol(channel, normalizedProtocol);
        long requestAmount = resolveRequestAmount(order);
        PaymentOrder reusable = paymentOrderMapper.selectReusablePendingForUpdate(
                normalizedOrderNo,
                paymentAppId,
                normalizedProtocol
        );
        if (reusable != null) {
            validateReusablePending(reusable, order, channel, requestAmount);
            return reusable;
        }

        PaymentOrder paymentOrder = new PaymentOrder();
        String paymentNo = newPaymentNo();
        paymentOrder.setPaymentNo(paymentNo);
        paymentOrder.setOrderNo(normalizedOrderNo);
        paymentOrder.setPaymentAppId(paymentAppId);
        paymentOrder.setChannel(channel);
        paymentOrder.setProtocol(normalizedProtocol);
        paymentOrder.setChannelMerchantOrderNo(paymentNo);
        paymentOrder.setRequestAmount(requestAmount);
        paymentOrder.setPaidAmount(0L);
        paymentOrder.setCurrency("CNY");
        paymentOrder.setStatus(PaymentOrderStatus.CREATED);
        paymentOrder.setDisposition(PaymentDisposition.NONE);
        paymentOrder.setRequestId(normalizedRequestId);
        paymentOrder.setRequestFingerprint(PaymentRequestFingerprint.sha256(
                normalizedOrderNo,
                paymentAppId,
                channel,
                normalizedProtocol,
                requestAmount
        ));
        paymentOrder.setRefundFrozenAmount(0L);
        paymentOrder.setRefundedAmount(0L);
        paymentOrder.setExpiresAt(order.getExpiresAt());
        if (paymentOrderMapper.insert(paymentOrder) != 1) {
            throw new BizException("支付尝试创建失败");
        }
        return paymentOrder;
    }

    @Override
    @Transactional
    public boolean claimForCreation(String paymentNo) {
        String normalizedPaymentNo = requireText(paymentNo, 64, "支付单号无效");
        PaymentOrder snapshot = paymentOrderMapper.selectByPaymentNo(normalizedPaymentNo);
        if (snapshot == null) {
            throw new NotFoundException("支付单不存在");
        }
        if (snapshot.getStatus() != PaymentOrderStatus.CREATED) {
            return false;
        }

        OrderInfo order = orderInfoMapper.selectByOrderNoForUpdate(snapshot.getOrderNo());
        if (order == null) {
            throw new ConflictException("支付单关联订单不存在");
        }
        PaymentOrder uncertain = paymentOrderMapper.selectUncertainByOrderNoForUpdate(order.getOrderNo());
        if (uncertain != null) {
            return false;
        }
        PaymentOrder locked = paymentOrderMapper.selectByPaymentNoForUpdate(normalizedPaymentNo);
        if (locked == null) {
            throw new NotFoundException("支付单不存在");
        }
        if (locked.getStatus() != PaymentOrderStatus.CREATED) {
            return false;
        }
        validatePayable(order);
        return paymentOrderMapper.updateStatus(
                normalizedPaymentNo,
                PaymentOrderStatus.CREATED,
                PaymentOrderStatus.CREATING
        ) == 1;
    }

    @Override
    @Transactional
    public PaymentOrder markPending(String paymentNo, String paymentParams) {
        return updateCreationResult(paymentNo, PaymentOrderStatus.PENDING, paymentParams);
    }

    @Override
    @Transactional
    public PaymentOrder markUnknown(String paymentNo) {
        return updateCreationResult(paymentNo, PaymentOrderStatus.UNKNOWN, null);
    }

    @Override
    @Transactional
    public PaymentOrder markFailed(String paymentNo, String failureDetails) {
        return updateCreationResult(paymentNo, PaymentOrderStatus.FAILED, failureDetails);
    }

    @Override
    public PaymentOrder getByPaymentNo(String paymentNo) {
        String normalizedPaymentNo = requireText(paymentNo, 64, "支付单号无效");
        PaymentOrder paymentOrder = paymentOrderMapper.selectByPaymentNo(normalizedPaymentNo);
        if (paymentOrder == null) {
            throw new NotFoundException("支付单不存在");
        }
        return paymentOrder;
    }

    @Override
    @Transactional(readOnly = true)
    public List<PaymentOrder> listForOrder(String orderNo, AuthUser requester) {
        if (requester == null || requester.getUserId() == null || requester.getRole() == null) {
            throw new ForbiddenException("无权访问支付单");
        }
        String normalizedOrderNo = requireText(orderNo, 64, "订单号无效");
        OrderInfo order = orderInfoMapper.selectByOrderNo(normalizedOrderNo);
        if (order == null) {
            throw new NotFoundException("订单不存在");
        }
        if (requester.getRole() != UserRole.ADMIN
                && !Objects.equals(requester.getUserId(), order.getUserId())) {
            throw new ForbiddenException("无权访问该订单的支付单");
        }
        List<PaymentOrder> payments = paymentOrderMapper.selectByOrderNo(normalizedOrderNo);
        return payments == null ? Collections.emptyList() : payments;
    }

    private PaymentOrder updateCreationResult(
            String paymentNo,
            PaymentOrderStatus targetStatus,
            String paymentParams
    ) {
        String normalizedPaymentNo = requireText(paymentNo, 64, "支付单号无效");
        int updated = paymentOrderMapper.updateCreationResult(
                normalizedPaymentNo,
                PaymentOrderStatus.CREATING,
                targetStatus,
                paymentParams
        );
        if (updated == 0 && targetStatus != PaymentOrderStatus.UNKNOWN) {
            updated = paymentOrderMapper.updateCreationResult(
                    normalizedPaymentNo,
                    PaymentOrderStatus.UNKNOWN,
                    targetStatus,
                    paymentParams
            );
        }
        PaymentOrder current = paymentOrderMapper.selectByPaymentNo(normalizedPaymentNo);
        if (current == null) {
            throw new NotFoundException("支付单不存在");
        }
        if (updated == 1 || current.getStatus() == targetStatus) {
            return current;
        }
        if (current.getStatus() == PaymentOrderStatus.SUCCESS) {
            return current;
        }
        throw new ConflictException("支付单状态已变化，不能更新为" + targetStatus);
    }

    private void validateReplay(
            PaymentOrder existing,
            OrderInfo order,
            Long paymentAppId,
            String protocol
    ) {
        long requestAmount = resolveRequestAmount(order);
        String expectedFingerprint = PaymentRequestFingerprint.sha256(
                order.getOrderNo(),
                paymentAppId,
                existing.getChannel(),
                protocol,
                requestAmount
        );
        if (!Objects.equals(paymentAppId, existing.getPaymentAppId())
                || !protocol.equals(normalize(existing.getProtocol()).toUpperCase(Locale.ROOT))
                || !Objects.equals(requestAmount, existing.getRequestAmount())
                || !expectedFingerprint.equals(existing.getRequestFingerprint())) {
            throw new ConflictException("相同支付请求号的参数不一致");
        }
    }

    private void validateReusablePending(
            PaymentOrder existing,
            OrderInfo order,
            String channel,
            long requestAmount
    ) {
        if (existing.getStatus() != PaymentOrderStatus.PENDING
                || !Objects.equals(order.getOrderNo(), existing.getOrderNo())
                || !Objects.equals(channel, normalize(existing.getChannel()).toUpperCase(Locale.ROOT))
                || !Objects.equals(requestAmount, existing.getRequestAmount())) {
            throw new ConflictException("可复用支付尝试与当前订单不一致");
        }
    }

    private void validatePayable(OrderInfo order) {
        boolean waitPay = "WAIT_PAY".equals(order.getOrderStatus())
                || "NOTPAY".equals(order.getOrderStatus())
                || OrderStatus.NOTPAY.getType().equals(order.getOrderStatus());
        if (!waitPay
                || (StringUtils.hasText(order.getPayStatus()) && !"UNPAID".equals(order.getPayStatus()))
                || StringUtils.hasText(order.getEffectivePaymentNo())) {
            throw new ConflictException("订单当前状态不允许创建支付尝试");
        }
        if (order.getCloseRequestedAt() != null) {
            throw new ConflictException("订单正在关闭，不能创建支付尝试");
        }
        if (order.getExpiresAt() == null
                || order.getExpiresAt().getTime() - System.currentTimeMillis() < MIN_REMAINING_MILLIS) {
            throw new ConflictException("订单剩余支付时间不足60秒");
        }
    }

    private long resolveRequestAmount(OrderInfo order) {
        Long amount = order.getPayableAmount();
        if (amount == null && order.getTotalFee() != null) {
            amount = order.getTotalFee().longValue();
        }
        if (amount == null || amount < 0) {
            throw new ConflictException("订单应付金额无效");
        }
        return amount;
    }

    private void validateProtocol(String channel, String protocol) {
        if (PaymentConfigLoader.CHANNEL_WXPAY.equals(channel)
                && ("V2".equals(protocol) || "V3".equals(protocol))) {
            return;
        }
        if (PaymentConfigLoader.CHANNEL_ALIPAY.equals(channel) && "ALIPAY".equals(protocol)) {
            return;
        }
        throw new ConflictException("支付协议与支付渠道不匹配");
    }

    private void requireShoppingUser(AuthUser requester) {
        if (requester == null || requester.getRole() != UserRole.USER) {
            throw new ForbiddenException("管理员账号不参与购物");
        }
        if (requester.getUserId() == null) {
            throw new ForbiddenException("未登录用户不能创建支付尝试");
        }
    }

    private String requireText(String value, int maxLength, String message) {
        String normalized = normalize(value);
        if (!StringUtils.hasText(normalized) || normalized.length() > maxLength) {
            throw new ConflictException(message);
        }
        return normalized;
    }

    private String normalizeChannel(String channel) {
        String normalized = normalize(channel).toUpperCase(Locale.ROOT);
        if (!StringUtils.hasText(normalized)) {
            throw new ConflictException("支付应用未关联有效渠道");
        }
        return normalized;
    }

    private String normalize(String value) {
        return value == null ? "" : value.trim();
    }

    private String newPaymentNo() {
        return "PAY" + UUID.randomUUID().toString().replace("-", "").toUpperCase(Locale.ROOT);
    }
}
