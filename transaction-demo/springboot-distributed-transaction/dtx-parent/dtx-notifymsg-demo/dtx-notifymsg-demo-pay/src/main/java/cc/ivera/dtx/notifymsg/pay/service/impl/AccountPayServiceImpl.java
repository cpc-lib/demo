package cc.ivera.dtx.notifymsg.pay.service.impl;

import cc.ivera.dtx.notifymsg.pay.dao.AccountPayDao;
import cc.ivera.dtx.notifymsg.pay.entity.AccountPay;
import cc.ivera.dtx.notifymsg.pay.service.AccountPayService;
import lombok.extern.slf4j.Slf4j;
import org.apache.rocketmq.spring.core.RocketMQTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Slf4j
public class AccountPayServiceImpl implements AccountPayService {

    @Autowired
    RocketMQTemplate rocketMQTemplate;
    @Autowired
    AccountPayDao accountPayDao;

    @Transactional
    @Override
    public AccountPay insertAccountPay(AccountPay accountPay) {
        int result = accountPayDao.insertAccountPay(accountPay.getId(),
                accountPay.getAccountNo(), accountPay.getPayAmount(), "success");
        if (result > 0) {
            //发送通知
            rocketMQTemplate.convertAndSend("topic_notifymsg", accountPay);
            return accountPay;
        }
        return null;
    }

    @Override
    public AccountPay getAccountPay(String txNo) {
        return accountPayDao.findByIdTxNo(txNo);
    }
}
