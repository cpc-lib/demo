package cc.ivera.service.payment;

import cc.ivera.entity.OrderInfo;
import cc.ivera.entity.PaymentOrder;
import cc.ivera.enums.PaymentOrderStatus;
import cc.ivera.exception.ConflictException;
import cc.ivera.mapper.OrderInfoMapper;
import cc.ivera.security.AuthUser;
import cc.ivera.util.JsonUtils;
import org.springframework.stereotype.Service;

import java.util.Map;

@Service
public class PaymentInitiationServiceImpl implements PaymentInitiationService {

    private final PaymentAttemptService paymentAttemptService;

    private final PaymentChannelGateway paymentChannelGateway;

    private final OrderInfoMapper orderInfoMapper;

    public PaymentInitiationServiceImpl(
            PaymentAttemptService paymentAttemptService,
            PaymentChannelGateway paymentChannelGateway,
            OrderInfoMapper orderInfoMapper
    ) {
        this.paymentAttemptService = paymentAttemptService;
        this.paymentChannelGateway = paymentChannelGateway;
        this.orderInfoMapper = orderInfoMapper;
    }

    @Override
    public PaymentOrder initiate(
            String orderNo,
            Long paymentAppId,
            String protocol,
            String requestId,
            String remoteAddr,
            AuthUser requester
    ) {
        PaymentOrder paymentOrder = paymentAttemptService.createOrReplay(
                orderNo,
                paymentAppId,
                protocol,
                requestId,
                requester
        );
        if (paymentOrder.getStatus() != PaymentOrderStatus.CREATED) {
            return paymentOrder;
        }
        if (!paymentAttemptService.claimForCreation(paymentOrder.getPaymentNo())) {
            return paymentAttemptService.getByPaymentNo(paymentOrder.getPaymentNo());
        }

        try {
            OrderInfo order = orderInfoMapper.selectByOrderNo(paymentOrder.getOrderNo());
            if (order == null) {
                throw new ConflictException("支付单关联订单不存在");
            }
            Map<String, Object> paymentParams = paymentChannelGateway.create(
                    paymentOrder,
                    order,
                    remoteAddr
            );
            if (paymentParams == null) {
                throw new ConflictException("支付渠道未返回支付参数");
            }
            return paymentAttemptService.markPending(
                    paymentOrder.getPaymentNo(),
                    JsonUtils.toJson(paymentParams)
            );
        } catch (PaymentChannelRejectedException exception) {
            try {
                paymentAttemptService.markFailed(paymentOrder.getPaymentNo(), exception.getMessage());
            } catch (RuntimeException stateException) {
                exception.addSuppressed(stateException);
            }
            throw exception;
        } catch (RuntimeException exception) {
            try {
                paymentAttemptService.markUnknown(paymentOrder.getPaymentNo());
            } catch (RuntimeException stateException) {
                exception.addSuppressed(stateException);
            }
            throw exception;
        }
    }
}
