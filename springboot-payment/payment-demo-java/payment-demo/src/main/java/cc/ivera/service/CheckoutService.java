package cc.ivera.service;

import cc.ivera.vo.CheckoutResult;

public interface CheckoutService {

    CheckoutResult checkout(Long userId, Long paymentAppId, String checkoutRequestId);
}
