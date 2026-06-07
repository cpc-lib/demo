package cc.ivera.service;

import java.util.Map;

public interface PaymentInfoService {

    void createPaymentInfo(String plainText);

    void createPaymentInfoForAliPay(Map<String, String> params);
}
