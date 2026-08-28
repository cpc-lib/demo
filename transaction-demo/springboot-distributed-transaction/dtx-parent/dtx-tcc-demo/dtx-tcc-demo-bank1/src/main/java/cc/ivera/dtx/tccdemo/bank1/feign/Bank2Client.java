package cc.ivera.dtx.tccdemo.bank1.feign;

import org.dromara.hmily.annotation.Hmily;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestParam;

@FeignClient(value = "tcc-demo-bank2", fallback = Bank2Fallback.class)
public interface Bank2Client {
    @GetMapping("/bank2/transfer")
    //xid传递
    @Hmily
    Boolean test(@RequestParam("amount") Double amount);
}
