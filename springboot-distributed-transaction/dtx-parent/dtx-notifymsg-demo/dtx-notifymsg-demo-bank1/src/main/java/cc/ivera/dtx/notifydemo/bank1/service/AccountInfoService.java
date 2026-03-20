package cc.ivera.dtx.notifydemo.bank1.service;

import cc.ivera.dtx.notifydemo.bank1.entity.AccountPay;
import cc.ivera.dtx.notifydemo.bank1.model.AccountChangeEvent;

public interface AccountInfoService {

    void updateAccountBalance(AccountChangeEvent accountChange);

    AccountPay queryPayResult(String tx_no);
}
