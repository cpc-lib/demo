package cc.ivera.dtx.tccdemo.bank1.feign;

import org.springframework.stereotype.Component;

@Component
public class Bank2Fallback implements Bank2Client {
    @Override
    public Boolean test(Double amount) {
        return false;
    }
}
