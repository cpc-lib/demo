package cc.ivera.dtx.notifydemo.bank1.feign;

import cc.ivera.dtx.notifydemo.bank1.entity.AccountPay;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;

@FeignClient(value = "dtxnotifymsgdemopay", fallback = PayClientFallback.class)
public interface PayClient {
    @GetMapping("/pay/payresult/{txNo}")
    AccountPay queryPayResult(@PathVariable("txNo") String txNo);
}