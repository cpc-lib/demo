package cc.ivera.dtx.notifymsg.pay.service;

import cc.ivera.dtx.notifymsg.pay.entity.AccountPay;
import org.springframework.transaction.annotation.Transactional;

public interface AccountPayService {
    @Transactional
    AccountPay insertAccountPay(AccountPay accountPay);

    AccountPay getAccountPay(String txNo);
}
