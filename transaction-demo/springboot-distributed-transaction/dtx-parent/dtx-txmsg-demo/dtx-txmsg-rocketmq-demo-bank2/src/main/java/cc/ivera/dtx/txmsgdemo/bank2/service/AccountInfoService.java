package cc.ivera.dtx.txmsgdemo.bank2.service;

import cc.ivera.dtx.txmsgdemo.bank2.model.AccountChangeEvent;

public interface AccountInfoService {
    void addAccountInfoBalance(AccountChangeEvent accountChangeEvent);
}
