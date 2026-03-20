package cc.ivera.dtx.tccdemo.bank2.service;

import org.dromara.hmily.annotation.Hmily;
import org.springframework.transaction.annotation.Transactional;

public interface AccountInfoService {
    @Transactional
    @Hmily(confirmMethod = "confirmMethod", cancelMethod = "cancelMethod")
    void updateAccountBalance(String accountNo, Double amount);
}
