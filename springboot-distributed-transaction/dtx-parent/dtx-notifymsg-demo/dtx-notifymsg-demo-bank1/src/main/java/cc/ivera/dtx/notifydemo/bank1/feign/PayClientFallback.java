package cc.ivera.dtx.notifydemo.bank1.feign;

import cc.ivera.dtx.notifydemo.bank1.entity.AccountPay;
import org.springframework.stereotype.Component;

@Component
public class PayClientFallback implements PayClient {
    @Override
    public AccountPay queryPayResult(String txNo) {
        AccountPay accountPay = new AccountPay();
        accountPay.setResult("fail");
        return accountPay;
    }
}