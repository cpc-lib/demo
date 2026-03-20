package cc.ivera.dtx.tccdemo.bank2.service.impl;

import cc.ivera.dtx.tccdemo.bank2.dao.AccountInfoDao;
import cc.ivera.dtx.tccdemo.bank2.service.AccountInfoService;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hmily.annotation.Hmily;
import org.dromara.hmily.core.concurrent.threadlocal.HmilyTransactionContextLocal;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AccountInfoServiceImpl implements AccountInfoService {

    @Autowired
    private AccountInfoDao accountInfoDao;

    @Override
    @Transactional
    @Hmily(confirmMethod = "confirmMethod", cancelMethod = "cancelMethod")
    public void   updateAccountBalance(String accountNo, Double amount) {
        String localTradeNo = HmilyTransactionContextLocal.getInstance().get().getTransId();
        log.info("******** Bank2 Service Begin try ..." + localTradeNo);
    }

    @Transactional
    public void confirmMethod(String accountNo, Double amount) {

        String localTradeNo = HmilyTransactionContextLocal.getInstance().get().getTransId();

        log.info("******** Bank2 Service commit... " + localTradeNo);

        //幂等性校验，已经执行过了，什么也不用做
        if (accountInfoDao.isExistConfirm(localTradeNo) > 0) {
            log.info("******** Bank2 已经执行过confirm... 无需再次confirm " + localTradeNo);
            return;
        }

        //正式增加金额
        accountInfoDao.addAccountBalance(accountNo, amount);

        //添加confirm日志
        accountInfoDao.addConfirm(localTradeNo);
    }

    @Transactional
    public void cancelMethod(String accountNo, Double amount) {
        String localTradeNo = HmilyTransactionContextLocal.getInstance().get().getTransId();
        log.info("******** Bank2 Service begin cancel... " + localTradeNo);
    }
}