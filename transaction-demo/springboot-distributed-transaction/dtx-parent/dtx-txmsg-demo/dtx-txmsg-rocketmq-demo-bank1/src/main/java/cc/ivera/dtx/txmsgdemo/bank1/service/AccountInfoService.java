package cc.ivera.dtx.txmsgdemo.bank1.service;

import cc.ivera.dtx.txmsgdemo.bank1.model.AccountChangeEvent;

public interface AccountInfoService {

    public void sendUpdateAccountBalance(AccountChangeEvent accountChangeEvent);

    public void doUpdateAccountBalance(AccountChangeEvent accountChangeEvent);

}
