package cc.ivera.service.payment;

import cc.ivera.entity.PaymentOrder;
import cc.ivera.security.AuthUser;

public interface PaymentInitiationService {

    PaymentOrder initiate(
            String orderNo,
            Long paymentAppId,
            String protocol,
            String requestId,
            String remoteAddr,
            AuthUser requester
    );
}
