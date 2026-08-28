package cc.ivera.dtx.seatademo.bank2.service.impl;

import cc.ivera.dtx.seatademo.bank2.dao.AccountInfoDao;
import cc.ivera.dtx.seatademo.bank2.service.AccountInfoService;
import io.seata.core.context.RootContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class AccountInfoServiceImpl implements AccountInfoService {
    @Autowired
    AccountInfoDao accountInfoDao;
    private Logger logger = LoggerFactory.getLogger(AccountInfoServiceImpl.class);

    @Override
    @Transactional
    public void updateAccountBalance(String accountNo, Double amount) {
        logger.info("******** Bank2 Service Begin ... xid: {}", RootContext.getXID());
        //李四增加金额
        accountInfoDao.updateAccountBalance(accountNo, amount);
        //制造异常
        if (amount == 2) {
            throw new RuntimeException("bank1 make exception 2");
        }
    }

}
