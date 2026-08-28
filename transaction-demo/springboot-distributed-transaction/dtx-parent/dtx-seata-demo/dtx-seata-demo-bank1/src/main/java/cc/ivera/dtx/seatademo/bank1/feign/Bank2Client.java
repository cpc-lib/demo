package cc.ivera.dtx.seatademo.bank1.feign;

import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "seata-demo-bank2", fallback = Bank2ClientFallback.class)
public interface Bank2Client {
    @GetMapping("/bank2/transfer")
    public String transfer(@RequestParam("amount") Double amount);
}