package cc.ivera.service.payment;

import cc.ivera.entity.PaymentOrder;
import cc.ivera.security.AuthUser;

import java.util.List;

public interface PaymentAttemptService {

    PaymentOrder createOrReplay(
            String orderNo,
            Long paymentAppId,
            String protocol,
            String requestId,
            AuthUser requester
    );

    boolean claimForCreation(String paymentNo);

    PaymentOrder markPending(String paymentNo, String paymentParams);

    PaymentOrder markUnknown(String paymentNo);

    PaymentOrder markFailed(String paymentNo, String failureDetails);

    PaymentOrder getByPaymentNo(String paymentNo);

    List<PaymentOrder> listForOrder(String orderNo, AuthUser requester);
}
