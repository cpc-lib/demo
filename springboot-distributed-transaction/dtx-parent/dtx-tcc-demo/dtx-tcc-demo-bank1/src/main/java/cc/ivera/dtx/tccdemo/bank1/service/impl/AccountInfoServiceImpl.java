package cc.ivera.dtx.tccdemo.bank1.service.impl;

import cc.ivera.dtx.tccdemo.bank1.dao.AccountInfoDao;
import cc.ivera.dtx.tccdemo.bank1.feign.Bank2Client;
import cc.ivera.dtx.tccdemo.bank1.service.AccountInfoService;
import lombok.extern.slf4j.Slf4j;
import org.dromara.hmily.annotation.Hmily;
import org.dromara.hmily.common.exception.HmilyRuntimeException;
import org.dromara.hmily.core.concurrent.threadlocal.HmilyTransactionContextLocal;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AccountInfoServiceImpl implements AccountInfoService {
    private Logger logger = LoggerFactory.getLogger(AccountInfoServiceImpl.class);
    @Autowired
    private AccountInfoDao accountInfoDao;
    @Autowired
    private Bank2Client bank2Client;

    @Override
    @Transactional
    @Hmily(confirmMethod = "commit", cancelMethod = "rollback")
    public void updateAccountBalance(String accountNo, Double amount) {
        //事务id
        String transId = HmilyTransactionContextLocal.getInstance().get().getTransId();
        log.info("******** Bank1 Service begin try... " + transId);

        int existTry = accountInfoDao.isExistTry(transId);

        //try幂等校验
        if (existTry > 0) {
            log.info("******** Bank1 Service 已经执行try，无需重复执行，事务id:{} " + transId);
            return;
        }


        //try悬挂处理
        if (accountInfoDao.isExistCancel(transId) > 0 || accountInfoDao.isExistConfirm(transId) > 0) {
            log.info("******** Bank1 Service 已经执行confirm或cancel，悬挂处理，事务id:{} " + transId
            );
            return;
        }

        //从账户扣减
        if (accountInfoDao.subtractAccountBalance(accountNo, amount) <= 0) {
            //扣减失败
            throw new HmilyRuntimeException("bank1 exception，扣减失败，事务id:{}" + transId);
        }

        //增加本地事务try成功记录，用于幂等性控制标识
        accountInfoDao.addTry(transId);

        //远程调用bank2
        if (!bank2Client.test(amount)) {
            throw new HmilyRuntimeException("bank2Client exception，事务id:{}" + transId);
        }


        if (amount == 10) {//异常一定要抛在Hmily里面
            throw new RuntimeException("bank1 make exception 10");
        }

        log.info("******** Bank1 Service end try... " + transId);
    }

    @Transactional
    public void commit(String accountNo, double amount) {
        String localTradeNo = HmilyTransactionContextLocal.getInstance().get().getTransId();
        logger.info("******** Bank1 Service begin commit..." + localTradeNo);
    }

    @Transactional
    public void rollback(String accountNo, double amount) {
        String localTradeNo = HmilyTransactionContextLocal.getInstance().get().getTransId();
        log.info("******** Bank1 Service begin rollback... " + localTradeNo);

        //空回滚处理，try阶段没有执行什么也不用做
        if (accountInfoDao.isExistTry(localTradeNo) <= 0) {
            log.info("******** Bank1 try阶段失败... 无需rollback " + localTradeNo);
            return;
        }

        //幂等性，已经执行过了，什么也不用做校验
        if (accountInfoDao.isExistCancel(localTradeNo) > 0) {
            log.info("******** Bank1 已经执行过rollback... 无需再次rollback " + localTradeNo);
            return;
        }

        //再将金额加回账户
        accountInfoDao.addAccountBalance(accountNo, amount);

        //添加cancel日志，用于幂等性控制标识
        accountInfoDao.addCancel(localTradeNo);

        log.info("******** Bank1 Service end rollback... " + localTradeNo);
    }
}