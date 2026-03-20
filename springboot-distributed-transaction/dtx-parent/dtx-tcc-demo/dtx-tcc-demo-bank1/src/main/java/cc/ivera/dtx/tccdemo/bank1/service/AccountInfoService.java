package cc.ivera.dtx.tccdemo.bank1.service;

import org.dromara.hmily.annotation.Hmily;
import org.springframework.transaction.annotation.Transactional;

public interface AccountInfoService {
    @Transactional
    @Hmily(confirmMethod = "commit", cancelMethod = "rollback")
    void updateAccountBalance(String accountNo, Double amount);
}
