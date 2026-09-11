package cc.ivera.service.payment;

import cc.ivera.exception.BizException;

/**
 * The channel definitively rejected a payment creation request, so retrying the
 * same payment number cannot produce a different result.
 */
public class PaymentChannelRejectedException extends BizException {

    public PaymentChannelRejectedException(String message) {
        super(message);
    }

    public PaymentChannelRejectedException(String message, Throwable cause) {
        super(message, cause);
    }
}
