package cc.ivera.dtx.seatademo.bank1.service.impl;

import cc.ivera.dtx.seatademo.bank1.dao.AccountInfoDao;
import cc.ivera.dtx.seatademo.bank1.feign.Bank2Client;
import cc.ivera.dtx.seatademo.bank1.service.AccountInfoService;
import io.seata.core.context.RootContext;
import io.seata.spring.annotation.GlobalTransactional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountInfoServiceImpl implements AccountInfoService {
    @Autowired
    AccountInfoDao accountInfoDao;
    @Autowired
    Bank2Client bank2Client;
    private Logger logger = LoggerFactory.getLogger(AccountInfoServiceImpl.class);

    //张三转账
    @Override
    @GlobalTransactional
    @Transactional
    public void updateAccountBalance(String accountNo, Double amount) {
        logger.info("******** Bank1 Service Begin ... xid: {}", RootContext.getXID());
        //张三扣减金额
        accountInfoDao.updateAccountBalance(accountNo, amount * (-1));
        //向李四转账
        String remoteRst = bank2Client.transfer(amount);
        //远程调用失败
        if (remoteRst.equals("fallback")) {
            throw new RuntimeException("bank1 下游服务异常");
        }
        //人为制造错误
        if (amount == 3) {
            throw new RuntimeException("bank1 make exception 3");
        }
    }
}